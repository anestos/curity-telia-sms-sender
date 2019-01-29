package io.curity.identityserver.plugin.sms.telia.demo;

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.annotation.DefaultString;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.Json;
import se.curity.identityserver.sdk.service.WebServiceClient;

interface TeliaSmsConfiguration extends Configuration
{

    ExceptionFactory getExceptionFactory();

    WebServiceClient getWebServiceClient();

    Json getJson();

    @DefaultString("360")
    String getCorrelationId();

    @DefaultString("PayEx")
    String getOriginatingAddress();

}
