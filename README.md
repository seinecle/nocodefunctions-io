# Nocode functions: the repo holding i/o processes

Visit [Nocodefunctions](https://nocodefunctions.com) which is the web app making use of these processes

In this "parent" repo for all i/o processes, you will find:

- the data structure (the model) common to all i/o functions. Please visit the folder "[DataImportModel](https://github.com/seinecle/nocodefunctions-io/tree/main/DataImportModel)".

- the file 'pom.xml' which defines the versions of all the dependencies shared by the io processes

- one function which [extracts text from specific regions in pdf files](https://github.com/seinecle/nocodefunctions-io/blob/main/import_pdf/src/main/java/net/clementlevallois/importers/import_pdf/controller/PdfExtractorByRegion.java). Formally, this function should be hosted elsewhere, but I keep it here as it is really about file reading and parsing. 

# License

All resources in this repo are licensed '[Creative Commons Attribution 4.0 International Public License](https://creativecommons.org/licenses/by/4.0/)', which essentially means that **the code and the assets of these processes can be used and modified including for commercial purposes, provided an attribution is made to the author (Clement Levallois)**.


# The two other essential repos of nocodefunctions:

- [the web front](https://github.com/seinecle/nocodefunctions-web-app)
- [the data analysis functions](https://github.com/seinecle/functions-model-and-parent-build). 

# Author

[Clement Levallois](https://ioc.exchange/@seinecle)