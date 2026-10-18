package com.neuronovaapps.rutinago.domain.communication

/** Independent of RutinaGo's destinations; never changes the message builder. */
enum class CommunicationDestination(val label: String) {
    HOME("Inicio"), CATEGORIES("Categorías"), MY_VOCABULARY("Mi vocabulario"), SETTINGS("Configuración");
}

/** Only navigation. Message, vocabulary selection and presentation retain their existing owners. */
data class CommunicationNavigationState(
    val destination: CommunicationDestination = CommunicationDestination.HOME,
    val personalizing: Boolean = false,
    val homeOrganizationOrigin: CommunicationDestination? = null,
) {
    fun select(destination: CommunicationDestination) = copy(destination = destination, personalizing = false, homeOrganizationOrigin = null)
    fun personalize() = copy(personalizing = true)
    fun organizeHome() = copy(destination = CommunicationDestination.MY_VOCABULARY, homeOrganizationOrigin = destination)
    /** null exits to the module selector; an editor returns to its original tab. */
    fun back(): CommunicationNavigationState? = when {
        homeOrganizationOrigin != null -> CommunicationNavigationState(homeOrganizationOrigin)
        personalizing -> copy(personalizing = false)
        else -> null
    }
}
