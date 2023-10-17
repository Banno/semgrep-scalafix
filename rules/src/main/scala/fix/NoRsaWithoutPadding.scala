package fix

import scalafix.v1._
import scala.meta._

class NoRsaWithoutPadding extends SemanticRule("NoRsaWithoutPadding") {
  val cipherMatch = SymbolMatcher.exact("javax/crypto/Cipher#getInstance().")
  val noPaddingR = ".*RSA/.*/NoPadding.*".r
  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case cipherMatch(Term.Apply.After_4_6_0(_, args)) =>
        args.collect {
          case t: Lit.String if noPaddingR.findFirstIn(t.value).nonEmpty =>
            Patch.lint(NoRsaWithoutPaddingDiagnostic(t))
        }.asPatch
    }.asPatch
  }
}

final case class NoRsaWithoutPaddingDiagnostic(t: Tree) extends Diagnostic {
  override def message: String =
    "Using RSA without OAE padding may weaken encryption. Use `OAEPWithMD5AndMGF1Padding` instead."

  override def position: Position = t.pos

  override def categoryID: String = "noRsaWithoutPadding"
}
