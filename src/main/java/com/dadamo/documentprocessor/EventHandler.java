package com.dadamo.documentprocessor;

import com.dadamo.documentprocessor.event.DocumentReceivedEvent;
import com.dadamo.documentprocessor.service.EventValidationService;
import com.dadamo.documentprocessor.exception.ValidationException;
import com.dadamo.documentprocessor.service.DocumentProcessorService;
import com.dadamo.documentprocessor.service.FileSystemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

public final class EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(EventHandler.class);
    static final int NORMAL_TERMINATION_CODE = 0;
    static final int PROCESSING_FAILED_CODE = 1;
    static final int VALIDATION_FAILED_CODE = 2;
    static final int USAGE_ERROR_CODE = 42;


    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        if (args.length != 1) {
            LOG.error("usage: documentProcessor <event.json>");
            return USAGE_ERROR_CODE;
        }

        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var outputDir = Paths.get(System.getProperty("output.dir", "output"));
        var fileSystemService = new FileSystemService(outputDir);
        var validationService = new EventValidationService();
        var documentProcessorService = new DocumentProcessorService(fileSystemService, mapper);

        var eventFileName = args[0];

        try {
            var documentReceivedEvent = mapper.readValue(
                    Paths.get(eventFileName).toFile(),
                    DocumentReceivedEvent.class
            );

            validationService.validate(documentReceivedEvent);
            var documentProcessedEvent = documentProcessorService.process(documentReceivedEvent);
            LOG.info("DocumentProcessed {}", mapper.writeValueAsString(documentProcessedEvent));

            return NORMAL_TERMINATION_CODE;

        } catch (ValidationException e) {

            LOG.error("validation failed: {}", e.errors());

            return VALIDATION_FAILED_CODE;

        } catch (Exception e) {

            LOG.error("processing failed for {}", eventFileName, e);

            return PROCESSING_FAILED_CODE;
        }
    }
}
