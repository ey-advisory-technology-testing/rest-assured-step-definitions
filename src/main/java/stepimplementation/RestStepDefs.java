package stepimplementation;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Predicate;
import cucumber.CustomBindings;
import cucumber.TestContext;
import data.DataUtils;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.xml.sax.SAXException;
import utils.helpers.pageobjechelpers.PageObjectGenerator;
import utils.validation.HardOrSoftAssertion;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.*;

public class RestStepDefs {

    static {
        RestAssured.useRelaxedHTTPSValidation();
    }

    private static Response requestSenderOptions;
    private static ScriptEngine nashorn = (new ScriptEngineManager((ClassLoader) null)).getEngineByName("nashorn");
    public RequestSpecification requestSpecs = RestAssured.given();

    Scenario scenario;

    @Before
    public void test(Scenario scenario) {
        this.scenario = scenario;
    }

    TestContext testContext;

    public RestStepDefs(TestContext testContext) {
        this.testContext = testContext;

    }

    Map<String, Object> bodyJsonAsMap = new HashMap<>();
    Map<String, Object> bodyJsonAsMap1 = new HashMap<>();
    Map<String, Object> bodyJsonAsMap2 = new HashMap<>();
    Map<String, Object> bodyJsonListAsMap = new HashMap<>();

    List<Map<String, Object>> bodyJsonList = new ArrayList<>();

    @Then("^get \"(.+)\"$")
    public void getURL(String url) {
        requestSenderOptions = RestAssured.when().get(url);
    }


    @Then("^post ([^\"]*)$")
    public void postURLEnv(String url) {
        if (url.contains("LIST")) {
            bodyJsonList.add(bodyJsonAsMap1);
            bodyJsonList.add(bodyJsonAsMap2);
            bodyJsonListAsMap.put("crossReferenceList", bodyJsonList);
            System.out.println(bodyJsonListAsMap);
            requestSenderOptions = requestSpecs.body(bodyJsonListAsMap).post(DataUtils.getEnvironmentSpecificData(url));
        } else if (url.contains("ENQUIRY")) {
            requestSenderOptions = requestSpecs.post(DataUtils.getEnvironmentSpecificData(url));
        } else {
            requestSenderOptions = requestSpecs.body(bodyJsonAsMap).post(DataUtils.getEnvironmentSpecificData(url));
        }
    }

    @Then("^delete \"([^\"]*)\"$")
    public void deleteURLEnv(String url) {
        if (url.contains("LIST")) {
            bodyJsonList.add(bodyJsonAsMap1);
            bodyJsonList.add(bodyJsonAsMap2);
            bodyJsonListAsMap.put("crossReferenceList", bodyJsonList);
            System.out.println(bodyJsonListAsMap);
            requestSenderOptions = requestSpecs.body(bodyJsonListAsMap).delete(DataUtils.getEnvironmentSpecificData(url));
        } else {
            requestSenderOptions = requestSpecs.body(bodyJsonAsMap).delete(DataUtils.getEnvironmentSpecificData(url));
        }
    }

    @Then("^post \"([^\"]*)\"$")
    public void post(String url) {
        requestSenderOptions = requestSpecs.contentType("application/json").post(url);
    }

    @Then("^post ([^\"]*) where \"(.*)\" is set to (.*) from entry in \"(.*)\" collection of \"(.*)\" database$")
    public void postURLEnvWhere(String url, String key, String valueKeyInEntry, String entryCollection, String entryDatabase) {
        String value = JsonPath.read(testContext.get(entryCollection + "." + entryDatabase + ".entry"), PageObjectGenerator.getElementObject(valueKeyInEntry).getAccessName()).toString();

        requestSenderOptions = RestAssured.when().post(DataUtils.getEnvironmentSpecificData(url).replace("{" + key + "}", value));
    }

    @Then("^post ([^\"]*) where \"(.*)\" is set to \"(.*)\"$")
    public void postURLEnvWhere(String url, String key, String value) {
        requestSenderOptions = RestAssured.when().post(DataUtils.getEnvironmentSpecificData(url).replace("{" + key + "}", value));
    }

    @Then("^param \"(.+)\" = \"(.+)\"$")
    public void setParam(String key, String value) {
        requestSpecs.param(key, value);

    }

    @Then("^param ([^\"]*) = ([^\"]*)$")
    public void setParamNoQuote(String key, String value) {
        requestSpecs.param(key, value);

    }

    @Then("^body \"(.+)\" = \"(.+)\"$")
    public void setBody(String key, String value) {
        bodyJsonAsMap.put(key, value);

    }

    @Then("^request ([^\"].*)$")
    public void setRequestBody(String body) {
        requestSpecs.body(eval(body));

    }

    @Then("^header \"(.+)\" = \"(.+)\"$")
    public void setHeader(String key, String value) {
        requestSpecs.header(key, value);
    }

    @Then("^header ([^\"].+) = ([^\"].+)$")
    public void setHeaderNoQuote(String key, String value) {
        if (value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 2);
        }
        requestSpecs.header(key, value);
    }

    @Then("^status (.*)$")
    public void status(int status) {
        requestSenderOptions.
                then().
                statusCode(status);
    }

    @Then("^print response$")
    public void printResponse() {
        requestSenderOptions.prettyPrint();
    }

    @And("request list {string}")
    public void requestList(String arg0) {
        if (arg0.equals("1")) {
            bodyJsonAsMap1 = bodyJsonAsMap;
        } else if (arg0.equals("2")) {
            bodyJsonAsMap2 = bodyJsonAsMap;
        }
    }

    @And("request list body {string} = {string}")
    public void requestListBody(String key, String value) {
        bodyJsonAsMap1.put(key, value);
        bodyJsonAsMap2.put(key, value);
    }

    @And("request list body {string} = {string} , {string}")
    public void requestListBody(String key, String value1, String value2) {
        bodyJsonAsMap1.put(key, value1);
        if (value2.isEmpty()) {
            bodyJsonAsMap2.put(key, null);
        } else {
            bodyJsonAsMap2.put(key, value2);
        }
    }

    @When("^def (\\w+) = (.+)")
    public void def(String name, String expression) throws ScriptException {
        if (testContext.containsKey("bindings")) {
            Bindings bindings = (Bindings) testContext.get("bindings");
            bindings.put(name, eval(expression));
        } else {
            Bindings vars = new CustomBindings(scenario);
            vars.put(name, eval(expression));
            testContext.put("bindings", vars);
        }

    }

    public Object eval(String expression) {
        if (!testContext.containsKey("bindings")) {
            testContext.put("bindings", new CustomBindings(scenario));
        }
        if (expression.startsWith("{")) {
            Object context = JsonPath.parse(expression).read("$", new Predicate[0]);
            if (context instanceof LinkedHashMap) {
                LinkedHashMap map = (LinkedHashMap) context;
                for (Object key : map.keySet()) {
                    String value = map.get(key).toString();
                    if (value.startsWith("#(") || value.startsWith("(")) {

                        try {
                            map.put(key, nashorn.eval(value.replace("#", ""), (Bindings) testContext.get("bindings")));
                        } catch (ScriptException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return context;
        }
        // if(expression.startsWith("#(")||expression.startsWith("(")) {
        //if (testContext.containsKey("bindings")) {
        try {
            return nashorn.eval(expression, (Bindings) testContext.get("bindings"));
        } catch (ScriptException e) {
            e.printStackTrace();
        }
        return null;
        //  }
        // return nashorn.eval(expression);
        // }
        // return expression;
    }

    @When("^set request body (.*)")
    public void request(String body) throws ScriptException {
        Object o = eval(body);
        requestSpecs.contentType("application/json").body(JsonPath.parse(o).jsonString());
    }

    @When("^method (\\w+)")
    public void method(String method) {
        switch (method.toLowerCase()) {
            case "post":
                requestSenderOptions = requestSpecs.post();
                break;
            case "get":
                requestSenderOptions = requestSpecs.get();
                break;
            case "delete":
                requestSenderOptions = requestSpecs.delete();
                break;
        }
        scenario.attach(requestSenderOptions.asPrettyString(), "text/plain", "Response");
        requestSpecs = RestAssured.given();
    }

    @When("^match (.+) (==|contains|any|only|deep) (.*)")
    public void match(String expression, String op1, String rhs) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        if (rhs == null) {
            rhs = "";
        }
//String retVal = requestSenderOptions.getBody().jsonPath().get(expression).toString();
        String resultString = requestSenderOptions.asString();
        Object retVal = null;
        if (DataUtils.isJson(resultString)) {
            retVal = JsonPath.read(resultString, expression);
        } else {
            retVal = requestSenderOptions.getBody().xmlPath().get(expression);
        }
        if (retVal == null) {
            retVal = "null";
        }
        if (op1.equals("==")) {
            HardOrSoftAssertion.hardorsoft_AssertEquals(retVal.toString(), rhs);
        } else {
            HardOrSoftAssertion.hardorsoft_AssertTrue(retVal.toString().contains(rhs), retVal + " contains " + rhs);
        }
//        switch ()
//        HardOrSoftAssertion.hardorsoft_AssertEquals(actualValue,value);
//                this.context.match(m.type, m.name, m.path, m.expected);
    }

    @When("^callonce (.+)")
    public void callonce(String line) throws ScriptException {
        ScriptEngine nashorn = (new ScriptEngineManager((ClassLoader) null)).getEngineByName("nashorn");
        nashorn.eval(line);
    }

    //  @When("^url (coke|'https://reqres.in')")
    @When("^url (.*)")
    public void url(String url) {
        if (url.startsWith("'") && url.endsWith("'")) {
            url = url.substring(1, url.length() - 2);
        }
        requestSpecs.request().baseUri(url);
    }

    @When("^path (.*)")
    public void path(String url) {
        if (url.startsWith("'") && url.endsWith("'")) {
            url = url.substring(1, url.length() - 2);
        }
        requestSpecs.request().basePath(url);
    }

}
