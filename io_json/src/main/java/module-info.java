/*
 * Copyright Clement Levallois 2021-2023. License Attribution 4.0 Intertnational (CC BY 4.0)
 */

module io_json {
    requires chardet4j;
    requires org.slf4j;
    requires org.eclipse.parsson;
    requires jakarta.json;
    exports net.clementlevallois.io.io_json;
}