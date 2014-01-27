package nz.ac.auckland.integration.testing.mock;

import nz.ac.auckland.integration.testing.MorcBuilder;
import nz.ac.auckland.integration.testing.endpointoverride.EndpointOverride;
import nz.ac.auckland.integration.testing.predicate.MultiPredicate;
import nz.ac.auckland.integration.testing.processor.MultiProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.util.URISupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A general class for specifying an expectation for a consumer of the
 * given endpoint URI. Each expectation can have a name (to distinguish
 * between expectations on the same endpoint).
 * <p/>
 * By default messages are expected to be in strict order (they arrive in the
 * order that they are defined in the test). This can be relaxed by setting the
 * orderingType to something other than TOTAL; PARTIAL ordering will allow messages
 * to arrive at a point in time after they're expected (useful for asynchronous messaging)
 * and also NONE which will accept a matching expectation at any point in the test. It is
 * also possible to set endpoint ordering to false such that messages can arrive in any order
 * - this may be useful when you just care about a message arriving, and not providing a
 * meaningful/ordered response.
 * <p/>
 * By default, it is expected that each expectation will occur only
 * once on a given endpoint; by setting expectedMessageCount we can repeat
 * the same expectation multiple times following the ordering parameters above
 * (e.g. by default, an expectedMessageCount of 3 would expect 3 messages in
 * a row without messages arriving from any other endpoint).
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */

public class MockDefinition {

    private static final Logger logger = LoggerFactory.getLogger(MockDefinition.class);

    private String endpointUri;
    private boolean isEndpointOrdered = true;
    private OrderingType orderingType;
    private List<Processor> processors;
    private List<Predicate> predicates;
    private int expectedMessageCount;
    private RouteDefinition mockFeederRoute;
    private Predicate lenientSelector;
    private LenientProcessor lenientProcessor;
    private long assertionTime;
    private Collection<EndpointOverride> endpointOverrides = new ArrayList<>();

    public enum OrderingType {
        TOTAL,
        PARTIAL,
        NONE
    }

    /**
     * @return The ordering type this expectation requires to be satisfied; the default is TOTAL
     *         which means it must arrive in the exact order it was specified. PARTIAL means it must
     *         arrive after it was defined. NONE means that it can arrive at any time at all during the
     *         test. The actual test execution will manage these differing ordering requirements between
     *         the different endpoints
     */
    public OrderingType getOrderingType() {
        return orderingType;
    }

    /**
     * @return The Camel-formatted URI that should be listened to for messages and requests
     */
    public String getEndpointUri() {
        return endpointUri;
    }

    /**
     * @return Whether messages arriving at this endpoint have to arrive in order
     */
    public boolean isEndpointOrdered() {
        return isEndpointOrdered;
    }

    public List<Processor> getProcessors() {
        return Collections.unmodifiableList(processors);
    }

    public List<Predicate> getPredicates() {
        return Collections.unmodifiableList(predicates);
    }

    public int getExpectedMessageCount() {
        return expectedMessageCount;
    }

    public RouteDefinition getMockFeederRoute() {
        return mockFeederRoute;
    }

    public Predicate getLenientSelector() {
        return lenientSelector;
    }

    public LenientProcessor getLenientProcessor() {
        return lenientProcessor;
    }

    public long getAssertionTime() {
        return assertionTime;
    }

    public Collection<EndpointOverride> getEndpointOverrides() {
        return endpointOverrides;
    }

    //public static class MockDefinitionBuilder<Builder extends MockDefinitionBuilder<Builder>> {
    public static class MockDefinitionBuilder<Builder extends MorcBuilder<Builder>> extends MorcBuilder<Builder> {

        private OrderingType orderingType = OrderingType.TOTAL;
        private boolean isEndpointOrdered = true;
        private Predicate lenientSelector = null;
        private int expectedMessageCount = 1;
        private RouteDefinition mockFeederRoute = null;
        private Class<? extends LenientProcessor> lenientProcessorClass = LenientProcessor.class;
        private LenientProcessor lenientProcessor;

        //these will be populated during the build
        private List<Predicate> predicates;
        private List<Processor> processors;

        private long assertionTime = 15000l;

        /**
         * @param endpointUri A Camel Endpoint URI to listen to for expected messages
         */
        public MockDefinitionBuilder(String endpointUri) {
            super(endpointUri);
        }

        public Builder expectedMessageCount(int expectedMessageCount) {
            this.expectedMessageCount = expectedMessageCount;
            return self();
        }

        /**
         * Specifies the ordering that this expectation requires (TOTAL, PARTIAL or NONE)
         */
        public Builder ordering(OrderingType orderingType) {
            this.orderingType = orderingType;
            return self();
        }

        /**
         * Specifies whether the endpoint expects messages in the order that they are defined.
         */
        public Builder endpointNotOrdered() {
            isEndpointOrdered = false;
            return self();
        }

        public Builder lenient() {
            return lenient(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    return true;
                }
            });
        }

        public Builder lenient(Predicate lenientSelector) {
            this.lenientSelector = lenientSelector;
            return self();
        }

        public Builder lenientProcessor(Class<? extends LenientProcessor> lenientProcessorClass) {
            this.lenientProcessorClass = lenientProcessorClass;
            return self();
        }

        public Builder mockFeederRoute(RouteDefinition mockFeederRoute) {
            this.mockFeederRoute = mockFeederRoute;
            return self();
        }

        public Builder assertionTime(long assertionTime) {
            this.assertionTime = assertionTime;
            return self();
        }

        public MockDefinition build(MockDefinition previousDefinitionPart) {

            if (expectedMessageCount < 0)
                throw new IllegalStateException("The expected message count for the mock definition on endpoint "
                        + getEndpointUri() + " must be at least 0");

            if (lenientSelector != null) {
                if (expectedMessageCount > 0)
                    logger.warn("Expectations for a lenient endpoint part {} will be ignored", getEndpointUri());

                expectedMessageCount = 0;

                try {
                    lenientProcessor = lenientProcessorClass.getDeclaredConstructor(List.class)
                            .newInstance(Collections.unmodifiableList(getProcessors()));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                        | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
            }

            if (previousDefinitionPart == null) {
                //set up a default expectation feeder route (sending to a mock will be added later)
                if (mockFeederRoute == null) mockFeederRoute = new RouteDefinition().convertBodyTo(String.class);
                mockFeederRoute.from(getEndpointUri());
            }

            if (lenientSelector != null && getPredicates().size() > 0)
                logger.warn("An endpoint uri {} mock definition part is marked as lenient but predicates have been " +
                        "provided - these will be ignored",getEndpointUri());

            if (lenientSelector == null) {
                predicates = getPredicates(expectedMessageCount);
                processors = getProcessors(expectedMessageCount);
            } else {
                predicates = new ArrayList<>();
                processors = new ArrayList<>();
            }

            if (previousDefinitionPart != null) {
                if (!previousDefinitionPart.getEndpointUri().equals(getEndpointUri()))
                    throw new IllegalStateException("The endpoints do not match for merging mock definition endpoint " +
                            previousDefinitionPart.getEndpointUri() + " with endpoint " + getEndpointUri());

                if (previousDefinitionPart.isEndpointOrdered() != isEndpointOrdered)
                    throw new IllegalStateException("The endpoint ordering must be the same for all mock definition parts of " +
                            "endpoint " + getEndpointUri());

                if (previousDefinitionPart.getOrderingType() != orderingType)
                    throw new IllegalStateException("The ordering type must be same for all mock definition parts on the endpoint "
                            + getEndpointUri());

                if (mockFeederRoute != null)
                    throw new IllegalStateException("The mock feeder route for the endpoint " + getEndpointUri() +
                            " can only be specified in the first mock definition part");

                if (previousDefinitionPart.getAssertionTime() != assertionTime) {
                    logger.warn("The assertion time for a subsequent mock definition part on endpoint {} has a different " +
                            "assertion time - the first will be used and will apply to the endpoint as a whole",getEndpointUri());
                    this.assertionTime = previousDefinitionPart.getAssertionTime();
                }

                if (lenientSelector != null && previousDefinitionPart.lenientSelector != null)
                    throw new IllegalStateException(getEndpointUri() + " can have only one part of a mock endpoint defined as lenient");

                //prepend the previous partPredicates/partProcessors onto this list ot make an updated expectation
                if (lenientSelector == null) {
                    predicates.addAll(0, previousDefinitionPart.getPredicates());
                    processors.addAll(0, previousDefinitionPart.getProcessors());
                } else {
                    predicates = previousDefinitionPart.getPredicates();
                    processors = previousDefinitionPart.getProcessors();
                }

                this.expectedMessageCount += previousDefinitionPart.getExpectedMessageCount();
                this.mockFeederRoute = previousDefinitionPart.getMockFeederRoute();
                this.isEndpointOrdered = previousDefinitionPart.isEndpointOrdered;
                //we know if this isn't null then we have checked this expectation hasn't set a selector/processor
                if (previousDefinitionPart.lenientSelector != null) {
                    this.lenientSelector = previousDefinitionPart.lenientSelector;
                    this.lenientProcessor = previousDefinitionPart.lenientProcessor;
                }
            }

            return new MockDefinition(this);
        }

        protected Builder self() {
            return (Builder) this;
        }

        protected OrderingType getOrderingType() {
            return this.orderingType;
        }
    }

    //any processors added should be thread safe!
    public static class LenientProcessor implements Processor {
        private List<Processor> processors;
        private AtomicInteger messageIndex = new AtomicInteger(0);

        public LenientProcessor(List<Processor> processors) {
            this.processors = processors;
        }

        public void process(Exchange exchange) throws Exception {
            if (processors.size() == 0) return;

            //the default implementation will cycle through the responses/partProcessors
            processors.get(messageIndex.getAndIncrement() % processors.size()).process(exchange);
        }
    }

    @SuppressWarnings("unchecked")
    private MockDefinition(MockDefinitionBuilder builder) {
        this.endpointUri = builder.getEndpointUri();
        this.orderingType = builder.orderingType;
        this.isEndpointOrdered = builder.isEndpointOrdered;
        this.processors = builder.processors;
        this.predicates = builder.predicates;
        this.expectedMessageCount = builder.expectedMessageCount;
        this.lenientProcessor = builder.lenientProcessor;
        this.lenientSelector = builder.lenientSelector;
        this.assertionTime = builder.assertionTime;
        this.endpointOverrides = builder.getEndpointOverrides();
        this.mockFeederRoute = builder.mockFeederRoute;
    }
}