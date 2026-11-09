import com.typesafe.config.{ConfigException, ConfigFactory, ConfigObject}
import org.typelevel.sbt.gha.GenerativePlugin
import org.typelevel.sbt.gha.GenerativeKeys._
import org.typelevel.sbt.gha.{UseRef, WorkflowJob, WorkflowStep}
import sbt._
import sbt.Keys._
import sbt.util.Logger

import scala.collection.JavaConverters._
import scala.util.Try

case class GHAStepRef(owner: String, repo: String)

trait GitHubStepPinningKeys {

  /** Map of GitHub action references to commit SHA hashes.
    *
    * Users can override this setting to add custom pins or update existing versions. The map key is
    * a GHAStepRef(owner, repo), and the value is the 40-character commit hash.
    *
    * Example:
    * {{{
    * gitHubActionStepPins += (GHAStepRef("myorg", "my-action") -> "abc123...")
    * }}}
    */
  lazy val gitHubActionStepPins: SettingKey[Map[GHAStepRef, String]] =
    settingKey[Map[GHAStepRef, String]](
      "Map of GitHub action references to commit hashes for pinning"
    )

  /** Enable or disable automatic pinning of GitHub action steps to commit hashes.
    *
    * When enabled, all GitHub actions in workflows will be pinned to specific commit SHAs for
    * improved security and reproducibility. Defaults to true.
    */
  lazy val gitHubActionStepPinningEnable: SettingKey[Boolean] =
    settingKey[Boolean]("Enable automatic pinning of GitHub action steps to commit hashes")
}

object GitHubStepPinningKeys extends GitHubStepPinningKeys

object GitHubStepPinning extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = GenerativePlugin

  object autoImport extends GitHubStepPinningKeys
  import autoImport._

  override lazy val buildSettings: Seq[Setting[_]] = Seq(
    gitHubActionStepPinningEnable := true,
    gitHubActionStepPins := defaultActionPins(sLog.value),

    // Transform all generated CI workflows
    githubWorkflowGeneratedCI := applyJobPinning(githubWorkflowGeneratedCI).value,

    // Transform added jobs
    githubWorkflowAddedJobs := applyJobPinning(githubWorkflowAddedJobs).value,

    // Transform download steps
    githubWorkflowGeneratedDownloadSteps := applyStepPinning(
      githubWorkflowGeneratedDownloadSteps
    ).value,

    // Transform upload steps
    githubWorkflowGeneratedUploadSteps := applyStepPinning(
      githubWorkflowGeneratedUploadSteps
    ).value,

    // Transform build steps
    githubWorkflowBuild := applyStepPinning(githubWorkflowBuild).value,

    // Transform job setup steps
    githubWorkflowJobSetup := applyStepPinning(githubWorkflowJobSetup).value,

    // Transform publish steps
    githubWorkflowPublish := applyStepPinning(githubWorkflowPublish).value,

    // Transform publish preamble steps
    githubWorkflowPublishPreamble := applyStepPinning(githubWorkflowPublishPreamble).value,

    // Transform publish postamble steps
    githubWorkflowPublishPostamble := applyStepPinning(githubWorkflowPublishPostamble).value
  )

  /** Helper to apply job pinning
    */
  def applyJobPinning(
      jobsSetting: SettingKey[Seq[WorkflowJob]]
  ): Def.Initialize[Seq[WorkflowJob]] = Def.setting {
    if (gitHubActionStepPinningEnable.value) {
      jobsSetting.value.map(job =>
        job.withSteps(pinWorkflowSteps(job.steps, gitHubActionStepPins.value, sLog.value))
      )
    } else {
      jobsSetting.value
    }
  }

  /** Helper to apply step pinning
    */
  def applyStepPinning(
      stepsSetting: SettingKey[Seq[WorkflowStep]]
  ): Def.Initialize[Seq[WorkflowStep]] = Def.setting {
    if (gitHubActionStepPinningEnable.value) {
      pinWorkflowSteps(stepsSetting.value.toList, gitHubActionStepPins.value, sLog.value)
    } else {
      stepsSetting.value
    }
  }

  private val CommitHash = "^[0-9a-fA-F]{40}$".r

  /** Pin workflow steps by replacing UseRef.Public references with commit hashes
    */
  private def pinWorkflowSteps(
      steps: List[WorkflowStep],
      pins: Map[GHAStepRef, String],
      log: Logger
  ): List[WorkflowStep] = {

    steps.map {
      case use: WorkflowStep.Use =>
        use.ref match {
          // Already pinned with commit hash (40 hex characters) - don't modify
          case UseRef.Public(_, _, CommitHash()) =>
            use
          // Not yet pinned - try to pin it
          case UseRef.Public(owner, repo, _) =>
            pins.get(GHAStepRef(owner, repo)) match {
              case Some(hash) =>
                // Create a new Use step with the pinned hash
                // Note: We preserve the original name to avoid breaking other plugins that pattern match on names
                WorkflowStep.Use(
                  name = use.name,
                  id = use.id,
                  ref = UseRef.Public(owner, repo, hash),
                  cond = use.cond,
                  env = use.env,
                  params = use.params,
                  continueOnError = use.continueOnError
                )
              case None =>
                // Log warning if no pin found, but don't fail
                log.warn(
                  s"""No pin found for GitHub action: $owner/$repo
                     |To pin this action, add it to your build.sbt:
                     |  gitHubActionStepPins += (GHAStepRef("$owner", "$repo") -> "<40-char-commit-hash>")""".stripMargin
                )
                use
            }
          // Non-public use, so docker or local reference
          case _ => use
        }
      case other => other
    }
  }

  /** Default action pins - loaded from github-pins.json resource.
    *
    * The file is a JSON map of the form:
    * {{{
    * {
    *   "owner/repo": { "version": "vX.Y.Z", "sha": "<40-char-commit-hash>" }
    * }
    * }}}
    *
    * Parsed with typesafe-config's `ConfigFactory`, since HOCON is a strict superset of JSON and
    * this dependency is already transitively available on sbt's plugin classpath.
    */
  private def defaultActionPins(log: Logger): Map[GHAStepRef, String] = {
    val resourcePath = "github-pins.json"

    Option(getClass.getClassLoader.getResourceAsStream(resourcePath)) match {
      case Some(stream) =>
        val reader = new java.io.InputStreamReader(stream, "UTF-8")
        try {
          ConfigFactory
            .parseReader(reader)
            .root()
            .entrySet()
            .asScala
            .flatMap { entry =>
              (entry.getKey.split("/", 2), entry.getValue) match {
                case (Array(owner, repo), pin: ConfigObject) if owner.nonEmpty && repo.nonEmpty =>
                  Try(pin.toConfig.getString("sha"))
                    .filter(_.nonEmpty)
                    .toOption
                    .map(sha => GHAStepRef(owner, repo) -> sha)
                case _ =>
                  None
              }
            }
            .toMap
        } catch {
          case e: ConfigException =>
            log.warn(
              s"Failed to parse $resourcePath, no default action pins loaded: ${e.getMessage}"
            )
            Map.empty
        } finally {
          // Closing the reader also closes the underlying stream
          reader.close()
        }
      case None =>
        log.warn(s"Could not find $resourcePath on the classpath, no default action pins loaded")
        Map.empty
    }
  }
}
