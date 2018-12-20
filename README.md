# Realwave Feign Commons

This lib provides a standardized way to declare a feign interface to our REST's API, providing the follows features:
* A wrapper to your current object response allowing uses lib's error decoder and response decoder, so you don't need to care about strategies of error treatments.
* Always logs (info or error) your Request and Response with customized module name.
* Allows obfuscate the logs above, providing a configurable obfuscation for json fields, path params and headers.
* Allows to extract the current controller API to a common interface to be shared with feign clients without break any current integration.
* Standardized error return with extensible metadata.
* Allow dynamic host resolution to your API.
# Table of contents
1. [When use it](#when)
2. [How configure](#howconfig)
3. [Basics](#basics)
4. [Migrating an Existing Controller to a Common Interface](#migratingcontroller)
	1. [Configuration of Your Feign Client](#configcontroller)
5. [Declaring Only Feign Interface](#migratinginterface)
	1. [Configuration of Your Feign Client](#configinterface)
6. [Advanced Configurations](#advanced)
	1. [Getting Feing.Builder from FeignConnectorConfigHelper](#feingbuilder)
	2. [Setting a different Jackson ObjectMapper](#objectmapper)
	3. [Configuring Log Obfuscator](#obfuscator)
	4. [Configuring Host Dynamic Resolution](#dynamicresolver)
7. [Using Your New Client](#clientuse)

## When use it<a name="when"></a>
* You have empty or json objects returns for your API. Other formats will be not fully supported.
* You have a standard error output: code and message to business errors and a map of lists of errors caused by field validations or you want to. The standard error responses to RW is ErrorMessageResponse in realwave-exception-handler lib.
* You have lot of feign.codec.ErrorDecoder or feign.codec.Decoder in your micro-service environment and want to reduce the boilerplated code. 

## Basics<a name="basics"></a>
The base of this lib is a *ClientResponse* class that acts like a basic Either class concept, but simplify the left and right concept to a success and error approach. The success response is defined by the type param of this class (e.g ClientResponse<WalletCreditCardAdded>) and errors are implemented by ErrorResponse, a class based into ErrorMessageResponse class, but decoupled to more flexibility.

This class when serialized will extract all fields, response or success, to the root level. So all payload will still the same. It is applied if you want to use ClientResponse was response and was thought to allow extract your controller's methods to an common interface used by feign clients and your controller without break any old API's consumer. 

All success response will be decoded using the type param class to parse as json the body and rebuilding a ClientResponse with the httpstatus returned. Any extra fields on request body will be ignored without fail by default.

The errors will be decoded trying to parse body as json to ErrorResponse class. If fails another ErrorResponse object will be created using the prefix propertie plus http status as code and a default message. So any exception will result in a valid error response and all process will be logged. When ErrorResponse is ready, a ClientResponse will be created and the http status will be set.

To allow more flexibility in ErrorResponse, all extra fields on json response will be setted as key-value map called metadata. The key will be the field name and the value will be a raw type or a LinkedHadhMap to objects (Jackson Strategy to deserialize Objects). So you can return more information if needed, but in other hand your client need to know how extract it.

Clients of your API will be able to use the ClientResponse fold method to route errors and success, extract the status using isError(), isBadRequest, isNotFound, getHttpStatus or getting directly the result using get (success) or error(error). See more in [Using Your New Client](#clientuse)
<blockquote style="background-color:#FFFEAC; color:black">the get method is only to success response. Get with a error ClientResponse will throw IllegalStateException to avoid wrong false-positive codding</blockquote>

## How configure<a name="howconfig"></a>
First import the lib on your project's pom.xml with the latest version:
```xml
<dependency>
	<groupId>br.com.zup.realwave</groupId>
    <artifactId>realwave-feign-commons</artifactId>
    <version>${version.number}</version>
</dependency>
```
<blockquote style="background-color:#FFFEAC; color:black">If you use realwave-financial-framework library you need update  to 2.0.2 > version</blockquote>

Now declare the follows configurations at your application.properties. You can configure the best values to fit in your project
```bash
feign.connect.timeout=10000
feign.read.timeout=50000
#Module name customization to better identify your log.
realwave.feing.module.client.name="Coffe Table Module"
#Module name error prefix to build unmapped error messages, such 500, 404, etc.
realwave.feing.module.client.error.prefix="COFF"
```

Make the package ```br.com.zup.realwave.fc.feign``` scannable in your Spring Boot Configuration class. Ex:
```java
@ComponentScan({"br.com.zup.realwave.external.wallet.app"
        , "br.com.zup.realwave.fc.framework.utils"
        , "br.com.zup.realwave.fc.feign"
        , "br.com.zup.realwave.external.wallet.connector.m4u"})
@Configuration
@Import(TrackingConfig.class)
public class ExternalWalletConfig extends WebMvcConfigurerAdapter {

}
```

Add this package into your log4j2.xml
```xml
<Logger name="br.com.zup.realwave.fc.feign" level="DEBUG" additivity="false">
	<AppenderRef ref="Console"/>
</Logger>
```

Now you're able to use the ClientResponse as wrapper to your success response in your API and the all lib's features. There are two main methods to do this migration. The first one is extracting from current controller a common interface, so client and controller will share the same requests/responses and the same REST mappings. The second way is create only the interface to feign clients. This is the main option if your controller returns Async responses that aren't currently supported by feign spring-boot module, so you cannot share the same interface. The following topics will shows how migrate and configure on these two scenarios.

## Migrating an Existing Controller to a Common Interface<a name="migratingcontroller"></a>

First, you need to extract your methods and spring REST mappings to a interface.So supposing that you have this controller implementation:
```java
@RestController
public class WalletController {
    @RequestMapping(method = RequestMethod.POST, value = "/v1/credit-cards", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createCreditCard(@Valid @RequestBody AddCreditCard addCreditCard){
        Either<ErrorMessageResponse,WalletCreditCardAdded> result = doStuffs(externalWalletAddCreditCard);
        return result.fold(
                error -> ResponseEntity.unprocessableEntity().body(error),
                success -> ResponseEntity.ok(success)

        );
    }
}
```
You need to rewrite into an interface with a little bit of modifications:
```java
    public interface CreditCardService {

    @RequestMapping(method = RequestMethod.POST, value = "/v1/credit-cards", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ClientResponse<WalletCreditCardAdded>> createCreditCard(@Valid @RequestBody AddCreditCard addCreditCard);
}
```
> Note that you still using the spring annotations for that. But your return type will be wrapped by ClientResponse with your successful response object as type param. It may sounds unecessary, but now you have a non-generic return and still supports the same returns as previous. ResponseEnttity still necessary to not loose http semantics, so you can build a response 422 returning the ClientResponse object. Also, this wrapper does not break any contract that you already have with a consumer of your API. 

With this new interface, you need to rewrite your controller:

```java
@RestController
public class WalletController  implements CreditCardService {

   @Override
   public ResponseEntity<ClientResponse<WalletCreditCardAdded>> createCreditCard(@Valid @RequestBody AddCreditCard addCreditCard){
        Either<ErrorMessageResponse, WalletCreditCardAdded> result = doStuffs(externalWalletAddCreditCard);
        return result.fold(
                error -> ResponseEntity.unprocessableEntity().body(ClientResponse.errorOf(ErrorResponse.of(error.getCode(), error.getMessage()))),
                success -> ResponseEntity.ok(ClientResponse.successOf(success))
        );
    }
}
```
### Configuration of Your Feign Client<a name="configcontroller"></a>
Now you have the same contract about your API implementation and Feign clients, so is a good idea put this commons objects of requests and response also the interface itself into a separated lib, to be imported for your API's future customers. Also can be a good idea provides a default class to construct your Feign bean client at runtime. It is not mandatory, since it can be done by the project that will use your API, but it is high recommended at least provide a documentation of minimal requirements about how configure your Feign.Builder, because you supposedly have more knowledge about the needs of obfuscation, json object mappers and so. This lib provides a helper to build clients with correctly configurations easily, but allows you to customize the builder if you want.

Using as example this previous API, here is how you can configure your Feign bean. Others ways to configure your feign builder will be showed later as advanced configuration.
```java
@Configuration
public class ExternalWalletConnectorConfig {

    @Bean
    public CreditCardService creditCardService(@Value("${wallet.rest.query.url}") String walletQueryUrl
                                               IamFeignInterceptor iamFeignInterceptor,
                                               FeignConnectorConfigHelper feignConnectorConfigHelper) {

		//obfuscate request and response logged (see about obfuscation topic)
        HttpObfuscator obfuscator = ConfigurableHttpObfuscator.builder()
                .jsonBodyFields("pan", "cardToken", "cvv")
                .pathSegments(Arrays.asList("/v1/credit-cards/${obfuscate}"))
                .build();
                
        return feignConnectorConfigHelper.config()
                .withSpringContract() //mandatory if you use spring annotations to share the same interface with controllers
                .withObfuscator(obfuscator)
                .withRequestInterceptor(new RealwaveRequestInterceptor(), iamFeignInterceptor) //RW interceptors
                .buildSimpleClient(CreditCardService.class, walletQueryUrl);
    }
}
```
## Declaring Only Feign Interface<a name="migratinginterface"></a>
Sometimes your application doesn't need or can't have a common interface either because you don't want be coupled with your client implementation or because your return type is not compatible with spring Feign limitations, like async returns. In that case, using this lib is a lot easy, because you don't have to do any modification in your controller.

The first step is declaring a interface with the same controller's signature. You can use default Feign annotations here, since your controller will not extends this interface. Also, you don't need to declare a ResponseEntity, because any response other than success will be a Feign exception. Using the same example, an interface would be like this:
```java
    public interface CreditCardService {
    
	@RequestLine("POST /v1/credit-cards")
    @Headers({"Content-Type: application/json"})
    ClientResponse<WalletCreditCardAdded> createCreditCard(@Valid AddCreditCard addCreditCard);
}
```
### Configuration of Your Feign Client<a name="configinterface"></a>
Same as migrating existing controller, is a good idea export your models and REST interface to a lib alone, allowing import only this lib to other client's projects  of your API. A configuration of Feign client in this case will be a lot more simple:
```java

@Configuration
public class ExternalWalletConnectorConfig {

    @Bean
    public CreditCardService creditCardService(@Value("${wallet.rest.query.url}") String walletQueryUrl
                                               IamFeignInterceptor iamFeignInterceptor,
                                               FeignConnectorConfigHelper feignConnectorConfigHelper) {

		//If you don't need/wanna obfuscate, you can ignore withObfuscator config.
        // If you are using defaults feign anottations in your client, you can drop the withSpringContract configs too
        return feignConnectorConfigHelper.config()
                .withRequestInterceptor(new RealwaveRequestInterceptor(), iamFeignInterceptor) //RW interceptors
                .buildSimpleClient(CreditCardService.class, walletQueryUrl);
    }
}
```

## Advanced Configurations<a name="advanced"></a>

### Getting Feing.Builder from FeignConnectorConfigHelper<a name="feingbuilder"></a>
You always can overwrite or extends the default configurations of Feing.Builder using FeignConnectorConfigHelper:
```java
    @Bean
    public CreditCardService creditCardService(IamFeignInterceptor iamFeignInterceptor,
                                               FeignConnectorConfigHelper feignConnectorConfigHelper) {

        
        Feign.Builder builder = feignConnectorConfigHelper.config()
                .withSpringContract()
                .withRequestInterceptor(new RealwaveRequestInterceptor(), iamFeignInterceptor)
                .feignBuilder();
        
        return builder.decode404()
                .target(CreditCardService.class, "http://myservice.com/api");
    }
```
Of course if you override decoders, etc. we cannot guarantee that will not occur parse failures, obfuscation or standardized log so use this only if you need customize some specific Feign behavior.

### Setting a different Jackson ObjectMapper<a name="objectmapper"></a>
If you need a specific ObjectMapper's config to parse your objects you can override the default one. But at least check the current default configurations to avoid some bizarre unmapped behavior, adding in your ObjectMapper most of defaults options.
```java
    @Bean
    public CreditCardService creditCardService(@Value("${wallet.rest.query.url}") String walletQueryUrl,
                                               IamFeignInterceptor iamFeignInterceptor,
                                               FeignConnectorConfigHelper feignConnectorConfigHelper) {
        ObjectMapper objectMapper = new ObjectMapper();
        /*these are the default configs of this lib.
        so with you want these options, you don't need to config ObjectMapper, just use
        feignConnectorConfigHelper.config()
         */
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"));
        objectMapper.setTimeZone(TimeZone.getTimeZone("UTC"));
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return feignConnectorConfigHelper.config(objectMapper)
                .withSpringContract()
                .withRequestInterceptor(new RealwaveRequestInterceptor(), iamFeignInterceptor)
                .buildSimpleClient(CreditCardService.class, walletQueryUrl);;
    }
```

### Configuring Log Obfuscator<a name="obfuscator"></a>
Requests and responses always will be logged by decoders (at least if you not overwrite decoders). Error responses will be logged with error level and successful responses as info level. If you don't want log all informations about requests and responses you can configure a obfuscator to hide secret infos. You can hide headers, specific json fields and path params with ConfigurableHttpObfuscator.

#### Json fields
All fields of json objects to be obfuscated of requests and responses in your API needs to be declared since the same HttpObfuscator will be used to all API methods. As example, assumes that you have the follow API:
```java
    @RequestMapping(method = RequestMethod.POST, value = "/v1/credit-cards", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ClientResponse<CreditCardAdded>> createCreditCard(@Valid @RequestBody AddCreditCard addCreditCard);

    @RequestMapping(method = RequestMethod.POST, value = "/v1/credit-cards/{externalId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ClientResponse<Void>> updateCreditCard(@Valid @RequestBody UpdateCreditCard updateCreditCard, @PathVariable("externalId") String externalId);

```
And your entities are:
```java
@Data
public class AddCreditCard{
	private String holder;
	private Address billingAddress;
	private String creditCardNumber;
	private String cvv;
}
@Data
public class Address{
	private String street;
	private String postalCode;
}
@Data
public class CreditCardAdded{
	private String externalId;
}
@Data
public class UpdateCreditCard{
	private String holder;
	private Address billingAddress;
    private Address secondaryAddress;
}
```
And your HttpObfuscator is configured declaring full path of any desired field:
```java
HttpObfuscator obfuscator = ConfigurableHttpObfuscator.builder()
                .jsonBodyFields("holder", "creditCardNumber", "cvv", "billingAddress.street", "externalId")
                .build();
```
Will result of obfuscation of the follows fields:

* **AddCreditCard**: holder, creditCardNumber, cvv, street field on billingAddress object.
* **CreditCardAdded**: externalId
* **UpdateCreditCard**: holder,  street field on billingAddress object (but not in secondaryAddress object);

**Note About Limitations**
> Only Json Objects (ObjectNode to be more precisily) will be processed by Obfuscator. Collections will be not obfuscated at this version
#### Headers
You just need to declare the desired header key name to make it obfuscated. Despite of http spec says that Headers name are case insensitive, on Feign http headers are stored as Map, so "X-application" and "x-application" aren't the same and you would need to declare both.
#### Path Segments
You can hide specific params of your path, like id references and so on. You only need to make a template of your URL path using ${obfuscate} at params that you want obfuscate and ${value} where you don't.
As example, suppose that you want to hide  the path param {externalId} of updateCreditCard method declared before:
```java
HttpObfuscator obfuscator = ConfigurableHttpObfuscator.builder()
                .pathSegments(Arrays.asList("/v1/credit-cards/${obfuscate}"))
                .build();
```

Another example: to obfuscate the CPF number of client with this REST Mapping ```/v1/customer/{cpf}/book/ref/{bookref}``` the template will be ```/v1/customer/${obfuscate}/book/ref/${value}``` and a call with ```/v1/customer/111111111111/book/ref/Ead1230as``` will result on ```/v1/customer/************/book/ref/Ead1230as``` logged URL.

#### Finally Gluing Together
The final configurations of our ConfigurableHttpObfuscator will be like that:
```java
HttpObfuscator obfuscator = ConfigurableHttpObfuscator.builder()
                .jsonBodyFields("holder", "creditCardNumber", "cvv", "billingAddress.street", "externalId")
                .headers("Authorization", "X-Application-Key")
                .pathSegments(Arrays.asList("/v1/credit-cards/${obfuscate}"))
                .build();
```
And the follow original request to updateCreditCard method:
```
POST http://localhost:8882/v1/credit-cards/01f85d071a92b1415ce7b8 HTTP/1.1
X-Customer-Id: 03237f28-8853-41a3-83d0-03457db6d014
X-Organization-Slug: zup
Content-Length: 1691
Authorization: Bearer aekajndeIUq3kjhasd21
X-Application-Id: 665b3ba9e5acf839c16061cb95f9c00612964061
X-Application-Key: Da09uqueajd093i2oi3jlkajsdlasd0923lLSDLA
Content-Type: application/json

{"holder":"Jordan Cardoso", "billingAddress":{"street":"Any Street","postalCode":"03092-2393"},"secondaryAddress":{"street":"Other Street","postalCode":"03092-2393"}}
```
Will be logged as:
```
INFO  ClientResponseDecoder - Wallet Command Module - Request was [
POST http://localhost:8882/v1/credit-cards/********************** HTTP/1.1
X-Customer-Id: 03237f28-8853-41a3-83d0-03457db6d014
X-Organization-Slug: zup
Content-Length: 1691
Authorization: ***************************
X-Application-Id: 665b3ba9e5acf839c16061cb95f9c00612964061
X-Application-Key: ****************************************
Content-Type: application/json

{"holder":"**************", "billingAddress":{"street":"**********","postalCode":"03092-2393"},"secondaryAddress":{"street":"Other Street","postalCode":"03092-2393"}}]. 
Response was [HTTP/1.1 200 OK
cache-control: no-cache, no-stage, must-revalidate
content-type: application/json
date: Tue Feb 20 19:16:24 GMT 2018
expires: Thu, 01 Jan 1970 00:00:00 GMT
pragma: no-cache
server: stubby4j/5.0.0 (HTTP stub server)
transfer-encoding: chunked
x-powered-by: Jetty(9.4.z-SNAPSHOT)
x-stubby-resource-id: 24
]
```
### Configuring Host Dynamic Resolution<a name="dynamicresolver"></a>
Sometimes is necessary resolve your host address of your Feign client at runtime. The most used way to do that is declaring an URL param into your Feign client's interface that is passed when the client method is invoked, like the example below:
```java
    @RequestLine("POST /payments")
    @Headers({"Content-Type: application/json"})
    ResponseEntity<AuthorizationResponse> authorize(URI var1, AuthorizationRequest var2);
```
But this modified signature blocks the possibility to share the same interface with Spring Boot Controllers. To to that you can build your client using FeignConnectorConfigHelper using a Dynamic Resolver that receives a DynamicHostResolver implementation. This implementation will be responsible to resolve the host string by overriding ```String getHost()``` method. You can retrieve this value from database, properties or whatever you want. This host resolution will be concatenated with path mapped into Feign interface at runtime, so make sure to return a valid value.

## Using Your New Client<a name="clientuse"></a>
If you are ready to use a Feign interface builded with this approach, you only need to inject the interface (CreditCardService in this example). If the lib already defines a default bean injetor for CreditCardService that's it, your bean will be already injected with right configurations, but if not, see previous topics "Configuration of Your Feign Client" and ask to the author about the right configuration aspects.

Once the bean injector as defined, you need to ensure that error responses will be catch by your connector on proper way. The best way to do this is wrapping by a try-catch block like this:
```java
@AutoWired
CreditCardService creditCardService;
...
  ...
public ClientResponse<WalletCreditCardAdded> create(AddCreditCard addCreditCard){
	try{
		return creditCardService.createCreditCard(addCreditCard);
	}catch(ClientResponseErrorDecoder.ClientResponseException e){
    	return e.error(WalletCreditCardAdded.class);
	}catch(Exception er){
    	//this block shouldn't be reached, but is always a good idea build a default response to unexpected failures of parsing weird jsons requests.
        log.error("Something useful to track errors", e);
    	//httpStatus will be null in this case.
		return ClientResponse.errorOf(ErrorResponse.of("MYMODULE-500", er.getMessage()));
    }
}
```
Or a more functional approach with io.vavr lib...
```java
@AutoWired
CreditCardService creditCardService;
...
  ...
public Either<ErrorMessageResponse, WalletCreditCardAdded> create(AddCreditCard addCreditCard){
	return Try.of(() -> creditCardService.createCreditCard(addCreditCard).result())
    .recover(x -> Match(x).of(
		Case($(instanceOf(ClientResponseErrorDecoder.ClientResponseException.class)), t -> t.error(ExternalWalletCreditCardAdded.class).result()),
		Case($(instanceOf(Exception.class)), this::buildError)
     )).get();
}
private <T> Either<ErrorMessageResponse, T> buildError(Exception e) {
    log.error("Something useful to track errors", e);
    //httpStatus will be null in this case.
    return Either.left(WalletErrorCode.UNKNOWN_ERROR.asErrorMessageResponse());
}
```

If you need to known wich kind of http error was returned to made some strategy to automatically recover or things like that, you can find some easy methods on ClientResponse:
* getHttpStatus: will return the decoded http status from response if your client could execute a request with response. If the exception is not generated by default ErrorDecoder, this value can be null.
* error(): get the left projection ErrorResponse, if exists.
* hasError: true if is error. Even if the response error isn't binded by standard fields, the ErrorDecoder will build an unmapped error, so code and message will never be null when a response error returns.
* isBadRequest: if is badrequest error response
* isNotFound: if is a not found error response
* get: returns success response object. Throws IllegalStateException if is an error response.
* fold: Like Either fold method, executes left or right functions depending on the result type (error or success).
