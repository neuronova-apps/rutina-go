package com.neuronovaapps.rutinago.domain.communication

/** Independent of RutinaGo's destinations; never changes the message builder. */
enum class CommunicationDestination(val label: String) {
    HOME("Inicio"), MY_VOCABULARY("Mi vocabulario"), SETTINGS("Configuración");
}

/** Only navigation. Message, vocabulary selection and presentation retain their existing owners. */
data class CommunicationNavigationState(
    val destination: CommunicationDestination = CommunicationDestination.HOME,
    val personalizing: Boolean = false,
    val homeOrganizationOrigin: CommunicationDestination? = null,
    val exploring: Boolean = false,
) {
    fun select(destination: CommunicationDestination) = CommunicationNavigationState(destination)
    fun personalize() = copy(personalizing = true, exploring = false, homeOrganizationOrigin = null)
    fun explore() = CommunicationNavigationState(exploring = true)
    fun organizeHome() = copy(destination = CommunicationDestination.MY_VOCABULARY, homeOrganizationOrigin = destination,
        personalizing = false, exploring = false)
    /** null exits to the module selector; an editor returns to its original tab. */
    fun back(): CommunicationNavigationState? = when {
        homeOrganizationOrigin != null -> CommunicationNavigationState(homeOrganizationOrigin)
        personalizing -> copy(personalizing = false)
        exploring -> CommunicationNavigationState()
        else -> null
    }
}
