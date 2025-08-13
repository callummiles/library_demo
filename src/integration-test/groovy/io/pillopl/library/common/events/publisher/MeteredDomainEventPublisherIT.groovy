package io.pillopl.library.common.events.publisher

import groovy.transform.CompileStatic
import io.micrometer.core.instrument.MeterRegistry
import io.pillopl.library.commons.events.DomainEvent
import io.pillopl.library.commons.events.publisher.MeteredDomainEventPublisher
import io.pillopl.library.lending.LendingTestContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import java.time.Instant

/**
 * Integration test for the MeteredDomainEventPublisher metrics functionality.
 * 
 * This test verifies that the domain event publisher correctly tracks and
 * records metrics for published events. The metrics are essential for
 * monitoring and observability of the domain event system, allowing
 * operators to track event publishing rates and identify potential issues.
 * 
 * The test ensures that each published event is properly counted and
 * tagged with the appropriate event type for detailed monitoring.
 */
@SpringBootTest(classes = [LendingTestContext.class, DomainEventsTestConfig.class])
class MeteredDomainEventPublisherIT extends Specification {

    @Autowired
    MeterRegistry meterRegistry

    @Autowired
    MeteredDomainEventPublisher publisher

    /**
     * Verifies that the MeteredDomainEventPublisher correctly counts and
     * tracks published domain events in the metrics registry. This test
     * publishes test events and verifies that the counter increments
     * appropriately, ensuring that the monitoring system can track
     * domain event publishing activity.
     */
    def "should count published events"() {
        when:
            publisher.publish(new TestEvent())
        then:
            countedEvents("domain_events", "name", "TestEvent") == 1.0
        when:
            publisher.publish(new TestEvent())
        then:
            countedEvents("domain_events", "name", "TestEvent") == 2.0
    }

    def countedEvents(String metricName, String key, String tag) {
        meterRegistry.counter(metricName, key, tag).count()
    }
}

@CompileStatic
class TestEvent implements DomainEvent {

    @Override
    UUID getEventId() {
        return null
    }

    @Override
    UUID getAggregateId() {
        return null
    }

    @Override
    Instant getWhen() {
        return null
    }
}
