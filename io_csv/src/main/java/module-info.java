/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/module-info.java to edit this template
 */

module net.clementlevallois.importers.import_csv {
    
    requires net.clementlevallois.io.model;
    requires univocity.parsers;
    requires org.jsoup;
    requires chardet4j;
            
    exports net.clementlevallois.importers.import_csv.controller;

}
