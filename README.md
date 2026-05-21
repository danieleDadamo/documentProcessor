Document Processor

Obtain or build the executable JAR file with maven:

    maven clean package
   
To run the application place the input event file in the same directory of the JAR, 
and place the pdf file in the _input_ subdirectory like this:

        -event.json

        -documentProcessor-1.0-SNAPSHOT.jar
        
        -input\example.pdf

Run:
        
        java -jar documentProcessor-1.0-SNAPSHOT.jar event.json

Exit codes:

        0 — success.

        non-zero — an error occurred.

Example event.json

    {
        "documentId": "123",
        "storageRef": "input/invoice.pdf",
        "metadata": {
            "type": "credit_note",
            "receivedAt": "2026-05-01T10:00:00Z"
        }
    }
