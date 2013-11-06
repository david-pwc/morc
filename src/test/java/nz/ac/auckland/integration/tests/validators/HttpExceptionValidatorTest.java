package nz.ac.auckland.integration.tests.validators;

import nz.ac.auckland.integration.testing.validator.HttpExceptionValidator;
import nz.ac.auckland.integration.testing.validator.Validator;
import org.apache.camel.Exchange;
import org.apache.camel.component.http.HttpOperationFailedException;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpExceptionValidatorTest extends Assert {

    @Test
    public void testNullExchange() throws Exception {
        HttpExceptionValidator validator = new HttpExceptionValidator.Builder().build();
        assertFalse(validator.validate(null));
    }

    @Test
    public void testSetResponseBodyHeadersStatusCode() throws Exception {
        Validator responseBodyValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return true;
            }
        };

        Validator responseHeadersValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return true;
            }
        };

        HttpExceptionValidator validator = new HttpExceptionValidator.Builder()
                .responseBodyValidator(responseBodyValidator)
                .responseHeadersValidator(responseHeadersValidator)
                .statusCode(500)
                .build();

        assertEquals(validator.getStatusCode(),500);
        assertEquals(validator.getResponseBodyValidator(),responseBodyValidator);
        assertEquals(validator.getResponseHeadersValidator(),responseHeadersValidator);
    }

    @Test
    public void testWrongException() throws Exception {
        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new IOException());
        assertFalse(new HttpExceptionValidator().validate(e));
    }

    @Test
    public void testDifferentStatusCode() throws Exception {
        Validator responseBodyValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return true;
            }
        };

        Validator responseHeadersValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return true;
            }
        };

        HttpExceptionValidator validator = new HttpExceptionValidator.Builder()
                .responseBodyValidator(responseBodyValidator)
                .responseHeadersValidator(responseHeadersValidator)
                .statusCode(500)
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new HttpOperationFailedException("uri",123,"status","location",null,null));
        assertFalse(validator.validate(e));
    }

    @Test
    public void testCorrectBody() throws Exception {
        Validator responseBodyValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return exchange.getIn().getBody(String.class).equals("foo");
            }
        };

        Validator responseHeadersValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return false;
            }
        };

        HttpExceptionValidator validator = new HttpExceptionValidator.Builder()
                .responseBodyValidator(responseBodyValidator)
                .responseHeadersValidator(responseHeadersValidator)
                .build();

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new HttpOperationFailedException("uri",123,"status","location",null,"foo"));
        assertFalse(validator.validate(e));
    }

    @Test
    public void testCorrectHeaders() throws Exception {
        Validator responseBodyValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                return false;
            }
        };

        Validator responseHeadersValidator = new Validator() {
            @Override
            public boolean validate(Exchange exchange) {
                String foo = exchange.getIn().getHeader("foo",String.class);
                String baz = exchange.getIn().getHeader("baz",String.class);
                return foo.equals("baz") && baz.equals("foo");
            }
        };

        HttpExceptionValidator validator = new HttpExceptionValidator.Builder()
                .responseBodyValidator(responseBodyValidator)
                .responseHeadersValidator(responseHeadersValidator)
                .build();

        Map<String,String> map = new HashMap<>();
        map.put("foo","baz");
        map.put("baz","moo");

        Exchange e = new DefaultExchange(new DefaultCamelContext());
        e.setException(new HttpOperationFailedException("uri",123,"status","location",map,"foo"));
        assertFalse(validator.validate(e));
    }
}