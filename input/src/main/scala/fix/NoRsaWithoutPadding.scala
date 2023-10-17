/*
rule = NoRsaWithoutPadding
 */
package fix

import javax.crypto.Cipher

object NoRsaWithoutPadding {
  Cipher.getInstance("RSA/None/NoPadding") /* assert: NoRsaWithoutPadding.noRsaWithoutPadding
                     ^^^^^^^^^^^^^^^^^^^^
  Using RSA without OAE padding may weaken encryption. Use `OAEPWithMD5AndMGF1Padding` instead.
   */

  Cipher.getInstance("RSA/ECB/NoPadding") /* assert: NoRsaWithoutPadding.noRsaWithoutPadding
                     ^^^^^^^^^^^^^^^^^^^
  Using RSA without OAE padding may weaken encryption. Use `OAEPWithMD5AndMGF1Padding` instead.
   */

  Cipher.getInstance("RSA/ECB/PKCS1Padding")

}
