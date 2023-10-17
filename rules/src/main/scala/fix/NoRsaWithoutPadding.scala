/*
 * Copyright 2023 Jack Henry & Associates, Inc.®
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
