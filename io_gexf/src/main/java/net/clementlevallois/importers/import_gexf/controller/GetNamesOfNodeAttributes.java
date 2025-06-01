/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.clementlevallois.importers.import_gexf.controller;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Table;
import org.gephi.io.importer.api.Container;
import org.gephi.io.importer.api.ContainerUnloader;
import org.gephi.io.importer.api.ImportController;
import org.gephi.io.importer.plugin.file.ImporterGEXF;
import org.gephi.io.importer.spi.FileImporter;
import org.gephi.io.processor.plugin.DefaultProcessor;
import org.gephi.project.api.ProjectController;
import org.openide.util.Lookup;

/**
 *
 * @author LEVALLOIS
 */
public class GetNamesOfNodeAttributes {

    /**
     * @param args the command line arguments
     */
    Path filePath;
    GraphModel gm;
    InputStream is;
    String gexf;
    private final Object lock = new Object();

    public GetNamesOfNodeAttributes(Path filePath) {
        this.filePath = filePath;
    }

    public GetNamesOfNodeAttributes(GraphModel gm) {
        this.gm = gm;
    }

    public GetNamesOfNodeAttributes(String gexf) {
        this.gexf = gexf;
    }

    public GetNamesOfNodeAttributes(InputStream is) {
        this.is = is;
    }

    public String returnNamesOfNodeAttributes() throws FileNotFoundException {
        if (gm == null) {
            load();
        }

        JsonObjectBuilder result = getNodeAttributeNames();
        String string = writeJsonObjectBuilderToString(result);
        return string;
    }

    private void load() throws FileNotFoundException {
        ProjectController projectController = null;
        Container container = null;

        synchronized (lock) {
            try {
                projectController = Lookup.getDefault().lookup(ProjectController.class);
                GraphController graphController = Lookup.getDefault().lookup(GraphController.class);
                ImportController importController = Lookup.getDefault().lookup(ImportController.class);
                projectController.newProject();
                if (filePath != null) {
                    File file = filePath.toFile();
                    container = importController.importFile(file);
                    container.closeLoader();
                } else if (is != null) {
                    FileImporter fi = new ImporterGEXF();
                    container = importController.importFile(is, fi);
                    container.closeLoader();
                } else if (gexf != null) {
                    FileImporter fi = new ImporterGEXF();
                    container = importController.importFile(new StringReader(gexf), fi);
                    container.closeLoader();
                }
                DefaultProcessor processor = new DefaultProcessor();
                processor.setWorkspace(projectController.getCurrentWorkspace());
                processor.setContainers(new ContainerUnloader[]{container.getUnloader()});
                processor.process();
                gm = graphController.getGraphModel();
            } finally {
                if (projectController != null) {
                    projectController.closeCurrentWorkspace();
                    projectController.closeCurrentProject();
                }
                if (container != null) {
                    container.closeLoader();
                }
            }
        }
    }

    private JsonObjectBuilder getNodeAttributeNames() {
        JsonObjectBuilder nodeNamesObjectBuilder = Json.createObjectBuilder();

        Table nodeTable = gm.getNodeTable();
        for (int i = 0; i < nodeTable.countColumns(); i++) {
            nodeNamesObjectBuilder.add(String.valueOf(i), nodeTable.getColumn(i).getTitle());
        }
        return nodeNamesObjectBuilder;
    }

    private String writeJsonObjectBuilderToString(JsonObjectBuilder jsBuilder) {
        Map<String, Boolean> configJsonWriter = new HashMap();
        configJsonWriter.put(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(configJsonWriter);
        Writer writer = new StringWriter();
        writerFactory.createWriter(writer).write(jsBuilder.build());

        String json = writer.toString();

        return json;
    }
}
