package io.curity.identityserver.plugin.sms.telia.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.HttpClient;
import se.curity.identityserver.sdk.service.Json;
import se.curity.identityserver.sdk.service.WebServiceClient;
import se.curity.identityserver.sdk.service.sms.SmsSender;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TeliaSmsSender implements SmsSender {
	private static Logger _logger = LoggerFactory.getLogger(TeliaSmsSender.class);
	private final ExceptionFactory _exceptionFactory;
	private final WebServiceClient _webServiceClient;
	private final Json _json;

	private static final String INVALID_NUMBER_ERROR = "invalid-phonenumber";

	private static final String unexpectedErrorMessage = "An unexpected error has occurred";
	private final String _correlationId;
	private final String _originatingAddress;

	public TeliaSmsSender(TeliaSmsConfiguration configuration)
	{
		_exceptionFactory = configuration.getExceptionFactory();
		_webServiceClient = configuration.getWebServiceClient();
		_json = configuration.getJson();
		_correlationId = configuration.getCorrelationId();
		_originatingAddress = configuration.getOriginatingAddress();

	}

	@Override
	public boolean sendSms(String toNumber, String msg) {
		_logger.trace("Sending SMS to number = {} using REST", toNumber);

		Map<String, Collection<String>> message = new HashMap<>();
		message.put("destinationAddress", Collections.singletonList(toNumber));
		message.put("userData", Collections.singletonList(msg));
		message.put("originatingAddress", Collections.singletonList(_originatingAddress));
		message.put("correlationId", Collections.singletonList(_correlationId));

		return executeRequest(message);
	}

	private boolean executeRequest(Map<String, Collection<String>> message)
	{

		try
		{
			HttpResponse response = _webServiceClient
					.withQueries(message)
					.request()
					.get()
					.response();

			int httpStatusCode = response.statusCode();

			if (httpStatusCode == 200)
			{
				return true;
			}

			String errorString = parseError(response);

			if (httpStatusCode == 400 && INVALID_NUMBER_ERROR.equals(errorString))
			{
				_logger.debug("Invalid phone number when attempting to send sms");
				return false;
			}

			_logger.warn("Failed to send SMS through REST backend. {}", errorString);
			throw _exceptionFactory.internalServerException(ErrorCode.EXTERNAL_SERVICE_ERROR);

		}
		catch (HttpClient.HttpClientException e)
		{
			_logger.warn("Error when communicating with SMS REST backend {}", e.getMessage());
			throw _exceptionFactory.internalServerException(
					ErrorCode.EXTERNAL_SERVICE_ERROR,
					unexpectedErrorMessage);

		}
	}

	private String parseError(HttpResponse response)
	{
		try
		{
			String responseBody = response.body(HttpResponse.asString());

			Map<String, Object> responseData = _json.fromJson(responseBody);

			if (responseData.containsKey("error"))
			{
				return responseData.get("error").toString();
			}
		}
		catch (Json.JsonException jse)
		{
			_logger.warn("Invalid syntax in error response from SMS rest backend");
		}

		return "";
	}
}
