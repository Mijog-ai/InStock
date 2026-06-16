package com.wms.gridlocator.i18n

enum class Language(val code: String, val displayName: String) {
    DE("de", "Deutsch"),
    EN("en", "English")
}

data class AppStrings(
    // General
    val appTitle: String,
    val loadingConfiguration: String,
    val logout: String,

    // Login
    val warehousePortal: String,
    val username: String,
    val password: String,
    val authenticateAccess: String,
    val invalidCredentials: String,
    val connectionError: String,

    // Zone selector
    val selectZone: String,
    val tapZoneToView: String,
    val zone: String,
    val locations: String,

    // Shelf grid
    val empty: String,
    val occupied: String,
    val backToZones: String,

    // Book in dialog
    val bookInTitle: String,
    val cancel: String,
    val bookLocation: String,
    val selectErpDelivery: String,
    val searchPlaceholder: String,
    val noMatches: String,
    val noRecentDeliveries: String,
    val item: String,
    val poNumber: String,
    val qty: String,
    val supplier: String,
    val date: String,
    val formDetails: String,
    val itemNumber: String,
    val type: String,
    val description: String,
    val itemNumberRequired: String,
    val poRequired: String,
    val qtyMustBePositive: String,
    val cellAtMaxCapacity: String,
    val bookedMessage: String,

    // Cell details dialog
    val occupiedStatus: String,
    val activeSlotsFormat: String,
    val cellNowEmpty: String,
    val close: String,
    val addBatch: String,
    val itemNo: String,
    val batch: String,
    val quantity: String,
    val bookedBy: String,
    val consume: String,
    val confirm: String,
    val consumedMessage: String,

    // Tabs
    val tabGridZones: String,
    val tabInputStock: String,
    val tabConsumeStock: String,

    // Consume stock screen
    val partNumber: String,
    val quantityToConsume: String,
    val findStock: String,
    val fifoSuggestion: String,
    val fifoSubtitle: String,
    val fifoEmptyTitle: String,
    val fifoEmptySubtitle: String,
    val noStockFound: String,
    val noActiveBookingsFor: String,
    val available: String,
    val take: String,
    val location: String,
    val confirmConsumption: String,
    val insufficientStock: String,
    val onlyXAvailable: String,
    val consumingFromLocations: String,

    // Language
    val language: String
)

object Strings {
    val DE = AppStrings(
        appTitle = "WMS Rasterplaner",
        loadingConfiguration = "Konfiguration wird geladen...",
        logout = "Abmelden",

        warehousePortal = "Lagerlogistik-Portal",
        username = "Benutzername",
        password = "Passwort",
        authenticateAccess = "Zugang authentifizieren",
        invalidCredentials = "Ungültiger Benutzername oder Passwort",
        connectionError = "Verbindungsfehler",

        selectZone = "Zone auswählen",
        tapZoneToView = "Zone antippen, um das Regalraster anzuzeigen",
        zone = "Zone",
        locations = "Plätze",

        empty = "FREI",
        occupied = "BELEGT",
        backToZones = "< Zurück zu Zonen",

        bookInTitle = "Einbuchen — Standortbuchung",
        cancel = "Abbrechen",
        bookLocation = "STANDORT BUCHEN",
        selectErpDelivery = "ERP-Lieferung auswählen",
        searchPlaceholder = "Artikel / Best.-Nr. suchen...",
        noMatches = "Keine Treffer für",
        noRecentDeliveries = "Keine aktuellen Lieferungen",
        item = "Artikel",
        poNumber = "Best.-Nr.",
        qty = "Menge",
        supplier = "Lieferant",
        date = "Datum",
        formDetails = "Formulardetails",
        itemNumber = "Artikelnummer",
        type = "Typ",
        description = "Beschreibung",
        itemNumberRequired = "Artikelnummer erforderlich",
        poRequired = "Best.-Nr. erforderlich",
        qtyMustBePositive = "Menge muss > 0 sein",
        cellAtMaxCapacity = "Zelle hat maximale Kapazität",
        bookedMessage = "Gebucht",

        occupiedStatus = "BELEGT",
        activeSlotsFormat = "aktive(r) Platz/Plätze",
        cellNowEmpty = "Zelle ist jetzt leer",
        close = "Schließen",
        addBatch = "+ CHARGE HINZUFÜGEN",
        itemNo = "Artikel-Nr.",
        batch = "Charge",
        quantity = "Menge",
        bookedBy = "Gebucht von",
        consume = "Verbrauchen",
        confirm = "Bestätigen",
        consumedMessage = "Einheiten verbraucht",

        tabGridZones = "Rasterplaner",
        tabInputStock = "Einlagern",
        tabConsumeStock = "Auslagern",

        partNumber = "Artikelnummer",
        quantityToConsume = "Menge zum Verbrauchen",
        findStock = "Bestand suchen",
        fifoSuggestion = "FIFO-Vorschlag",
        fifoSubtitle = "Artikelnummer und Menge eingeben — das System schlägt Lagerplätze nach FIFO vor (älteste zuerst).",
        fifoEmptyTitle = "FIFO-Verbrauch",
        fifoEmptySubtitle = "Artikelnummer suchen, um Lagerplätze nach Alter sortiert anzuzeigen",
        noStockFound = "Kein Bestand gefunden",
        noActiveBookingsFor = "Keine aktiven Buchungen für",
        available = "Verfügbar",
        take = "Entnehmen",
        location = "Lagerort",
        confirmConsumption = "Verbrauch bestätigen",
        insufficientStock = "Unzureichender Bestand",
        onlyXAvailable = "verfügbar, benötigt",
        consumingFromLocations = "Verbrauch von Lagerort(en)",

        language = "Sprache"
    )

    val EN = AppStrings(
        appTitle = "WMS Grid Locator",
        loadingConfiguration = "Loading configuration...",
        logout = "Logout",

        warehousePortal = "Warehouse Logistics Portal",
        username = "Username",
        password = "Password",
        authenticateAccess = "Authenticate Access",
        invalidCredentials = "Invalid username or password",
        connectionError = "Connection error",

        selectZone = "Select Zone",
        tapZoneToView = "Tap a zone to view shelf grid",
        zone = "Zone",
        locations = "locations",

        empty = "EMPTY",
        occupied = "OCCUPIED",
        backToZones = "< Back to Zones",

        bookInTitle = "Book In — Location Booking",
        cancel = "Cancel",
        bookLocation = "BOOK LOCATION",
        selectErpDelivery = "Select ERP Delivery",
        searchPlaceholder = "Search item / PO#...",
        noMatches = "No matches for",
        noRecentDeliveries = "No recent deliveries",
        item = "Item",
        poNumber = "PO#",
        qty = "Qty",
        supplier = "Supplier",
        date = "Date",
        formDetails = "Form Details",
        itemNumber = "Item Number",
        type = "Type",
        description = "Description",
        itemNumberRequired = "Item number required",
        poRequired = "PO# required",
        qtyMustBePositive = "Quantity must be > 0",
        cellAtMaxCapacity = "Cell at max capacity",
        bookedMessage = "Booked",

        occupiedStatus = "OCCUPIED",
        activeSlotsFormat = "active slot(s)",
        cellNowEmpty = "Cell is now empty",
        close = "Close",
        addBatch = "+ ADD BATCH",
        itemNo = "Item No.",
        batch = "Batch",
        quantity = "Quantity",
        bookedBy = "Booked By",
        consume = "Consume",
        confirm = "Confirm",
        consumedMessage = "units consumed",

        tabGridZones = "Grid Zones",
        tabInputStock = "Input Stock",
        tabConsumeStock = "Consume Stock",

        partNumber = "Part number",
        quantityToConsume = "Quantity to consume",
        findStock = "Find stock",
        fifoSuggestion = "FIFO Suggestion",
        fifoSubtitle = "Enter a part number and quantity — the system will suggest locations using FIFO (oldest first).",
        fifoEmptyTitle = "FIFO Consumption",
        fifoEmptySubtitle = "Search for a part number to see stock locations sorted oldest-first",
        noStockFound = "No stock found",
        noActiveBookingsFor = "No active bookings for",
        available = "Available",
        take = "Take",
        location = "Location",
        confirmConsumption = "Confirm consumption",
        insufficientStock = "Insufficient stock",
        onlyXAvailable = "available, need",
        consumingFromLocations = "Consuming from location(s)",

        language = "Language"
    )

    fun get(language: Language): AppStrings = when (language) {
        Language.DE -> DE
        Language.EN -> EN
    }
}
