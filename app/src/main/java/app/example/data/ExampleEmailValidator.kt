package app.example.data

// Example of a class that is used in traditional Dagger module
// Does not use Anvil contributed binding
class ExampleEmailValidator {
    fun isValidEmail(email: String): Boolean = email.contains("@")
}
