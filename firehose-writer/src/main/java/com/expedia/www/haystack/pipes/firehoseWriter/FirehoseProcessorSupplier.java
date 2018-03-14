package com.expedia.www.haystack.pipes.firehoseWriter;

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.expedia.open.tracing.Span;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FirehoseProcessorSupplier implements ProcessorSupplier<String, Span> {
    private final Logger firehoseProcessorLogger;
    private final FirehoseCountersAndTimer firehoseCountersAndTimer;
    private final Batch batch;
    private final AmazonKinesisFirehose amazonKinesisFirehose;
    private final FirehoseProcessor.Factory firehoseProcessorFactory;
    private final FirehoseConfigurationProvider firehoseConfigurationProvider;

    @Autowired
    public FirehoseProcessorSupplier(Logger firehoseProcessorLogger,
                                     FirehoseCountersAndTimer firehoseCountersAndTimer,
                                     Batch batch,
                                     AmazonKinesisFirehose amazonKinesisFirehose,
                                     FirehoseProcessor.Factory firehoseProcessorFactory,
                                     FirehoseConfigurationProvider firehoseConfigurationProvider) {
        this.firehoseProcessorLogger = firehoseProcessorLogger;
        this.firehoseCountersAndTimer = firehoseCountersAndTimer;
        this.batch = batch;
        this.amazonKinesisFirehose = amazonKinesisFirehose;
        this.firehoseProcessorFactory = firehoseProcessorFactory;
        this.firehoseConfigurationProvider = firehoseConfigurationProvider;
    }

    @Override
    public Processor<String, Span> get() {
        return new FirehoseProcessor(firehoseProcessorLogger, firehoseCountersAndTimer, batch, amazonKinesisFirehose,
                firehoseProcessorFactory, firehoseConfigurationProvider);
    }
}