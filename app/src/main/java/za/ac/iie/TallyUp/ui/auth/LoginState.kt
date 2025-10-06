@file:Suppress("unused", "PackageName")

package za.ac.iie.TallyUp.ui.auth

@Suppress("ConvertObjectToDataObject")
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}