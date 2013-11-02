package nz.ac.auckland.integration.testing.validator;

import org.apache.camel.Exchange;
import org.apache.camel.component.http.HttpOperationFailedException;
import org.apache.camel.impl.DefaultExchange;

/**
 * For validating the response exception is as expected
 *
 * @author David MacDonald <d.macdonald@auckland.ac.nz>
 */
public class HttpExceptionValidator implements Validator {

    private Validator responseBodyValidator;

    public HttpExceptionValidator(Validator responseBodyValidator) {
        this.responseBodyValidator = responseBodyValidator;
    }

    public HttpExceptionValidator() {

    }

    //todo check various HttpOperationFailedException

    public boolean validate(Exchange e) {
        if (e == null) return false;
        Throwable t = e.getException();
        if (!(t instanceof HttpOperationFailedException)) return false;

        if (responseBodyValidator == null) return true;

        String responseBody = ((HttpOperationFailedException) t).getResponseBody();

        //this is a bit of a hack to use other validators
        Exchange validationExchange = new DefaultExchange(e);
        e.getIn().setBody(responseBody);

        return responseBodyValidator.validate(validationExchange);
    }
}
