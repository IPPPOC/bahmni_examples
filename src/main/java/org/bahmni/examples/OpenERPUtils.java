package org.bahmni.examples;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import javax.xml.bind.DatatypeConverter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class OpenERPUtils {


    public static final String HOST = "localhost";
    public static final int PORT = 8069;
    public static final String SCHEME = "http";
    public static final String DATABASE = "openerp";
    public static final String USER = "admin";
    public static final String PASSWORD = "password";
    public static final String PATIENT_NAME = "Raj Malhotra";
    public static final String PATIENT_UUID = "6e6691b4-27c5-4733-9f26-342a28317423";

    public static void main(String[] args) throws Exception {
        OpenERPUtils  app = new OpenERPUtils();
        //app.getDatabaseList();
        int connectionId = app.login();
        Object[] customers = app.findCustomers(connectionId, PATIENT_NAME, PATIENT_UUID);
        List<Object> erpPartnerIds = Arrays.stream(customers).map(e -> ((Map) e).get("id")).collect(Collectors.toList());
        Map<Object, Object> saleOrdersForCustomers = new HashMap();
        Arrays.stream(customers).forEach(c -> saleOrdersForCustomers.put( ((Map) c).get("id"), ((Map) c).get("sale_order_ids")));
        for (Object p : erpPartnerIds) {
            Object orders = saleOrdersForCustomers.get(p);
            Object[] orderObjects = (Object[]) app.findOrders(connectionId, (int) p, (Object[]) orders);
            debug(orderObjects);
        }
        //app.getInvoices(connectionId);
    }

    private static void debug(Object[] orderObjects) {
        for (Object orderObject : orderObjects) {
            System.out.println("**************** Order Object Details *********** ");
            System.out.println(orderObject);
        }
    }

    public List<String> getDatabaseList() throws MalformedURLException, XmlRpcException {
        XmlRpcClient xmlrpcDb = new XmlRpcClient();

        XmlRpcClientConfigImpl xmlrpcConfigDb = new XmlRpcClientConfigImpl();
        xmlrpcConfigDb.setEnabledForExtensions(true);
        xmlrpcConfigDb.setServerURL(new URL(SCHEME, HOST, PORT, "/xmlrpc/db"));

        xmlrpcDb.setConfig(xmlrpcConfigDb);
        Vector<String> res = new Vector<String>();
        //Retrieve databases
        List<Object> params = new Vector<Object>();
        Object result = xmlrpcDb.execute("list", params);
        Object[] a = (Object[]) result;

        System.out.println(a.length);
        System.out.println(a.getClass());
        for (int i = 0; i < a.length; i++) {
            if (a[i] instanceof String) {
                res.addElement((String) a[i]);
            }
        }
        return res;
    }

    public int login() throws MalformedURLException, XmlRpcException {
        XmlRpcClient xmlrpcLogin = new XmlRpcClient();
        XmlRpcClientConfigImpl xmlrpcConfigLogin = new XmlRpcClientConfigImpl();
        xmlrpcConfigLogin.setEnabledForExtensions(true);
        xmlrpcConfigLogin.setServerURL(new URL(SCHEME,HOST,PORT,"/xmlrpc/common"));
        xmlrpcLogin.setConfig(xmlrpcConfigLogin);
        Object[] params = new Object[] {DATABASE,USER,PASSWORD};
        return (int) xmlrpcLogin.execute("login", params);

    }

    public Object findOrders(Integer connectionId, Integer erpCustomerId, Object[] saleOrderIds) throws MalformedURLException, XmlRpcException {
        XmlRpcClient xmlrpcClient = getXmlRpcClient();
        Object[] customerOrderIds = (saleOrderIds == null || saleOrderIds.length == 0)
                ? findSaleOrderIdsForCustomer(connectionId, erpCustomerId, xmlrpcClient)
                : saleOrderIds;

        if (customerOrderIds.length > 0) {
            List criteria = new Vector(); //criteria.add(69);
            Arrays.stream(customerOrderIds).forEach(e -> criteria.add(Integer.valueOf(e.toString()).intValue()));
            List<Object> orderReadParams = asList(
                    DATABASE, connectionId, PASSWORD, "sale.order", "read", criteria
            );
            return xmlrpcClient.execute("execute", orderReadParams.toArray());
        }

        return null;
    }

    private Object[] findSaleOrderIdsForCustomer(Integer connectionId, Integer erpCustomerId, XmlRpcClient xmlrpcClient) throws XmlRpcException {
        List criteria = new Vector();
        criteria.add(asList("partner_id", "=", erpCustomerId).toArray());
        List<Object> orderSearchParams = asList(
                DATABASE, connectionId, PASSWORD, "sale.order", "search", criteria
        );
        Object resultIds = xmlrpcClient.execute("execute", orderSearchParams.toArray());
        return (Object[]) resultIds;
    }


    private XmlRpcClient getXmlRpcClient() throws MalformedURLException {
        XmlRpcClient xmlrpcClient = new XmlRpcClient();

        XmlRpcClientConfigImpl xmlrpcConfig = new XmlRpcClientConfigImpl();
        xmlrpcConfig.setEnabledForExtensions(true);
        xmlrpcConfig.setServerURL(new URL(SCHEME,HOST,PORT,"/xmlrpc/object"));

        xmlrpcClient.setConfig(xmlrpcConfig);
        return xmlrpcClient;
    }

    private Object[] findCustomers(Integer connectionId, String customerName, String patientUuid) throws MalformedURLException, XmlRpcException {
        XmlRpcClient xmlrpcClient = getXmlRpcClient();

        List<Object> criteria = new ArrayList<Object>();
//        criteria.add(asList("name", "=", customerName).toArray());
//        criteria.add(asList("customer", "=", true).toArray());
        criteria.add(asList("uuid", "=", patientUuid).toArray());
        List<Object> customerSearchParams = asList(
                DATABASE, connectionId, PASSWORD, "res.partner", "search", criteria
        );
        Object customerIds = xmlrpcClient.execute("execute", customerSearchParams.toArray());

        criteria.clear();
        Arrays.stream((Object[]) customerIds).forEach(e -> criteria.add(Integer.valueOf(e.toString())));
        List<Object> readParams = asList(
                DATABASE, connectionId, PASSWORD, "res.partner", "read", criteria
        );

        /**
         result - array of hashmap,
         "ref":patient identifier (e.g GAN203006)
         "uuid": emr patient uuid (e.g. "uuid" -> "6e6691b4-27c5-4733-9f26-342a28317423")
         "sale_order_ids": array of int objects
         "invoice_ids": : array of int objects
         "id":ERP partner id (int)
         */
        Object result = xmlrpcClient.execute("execute", readParams.toArray());
        return (Object[]) result;

    }

    //Not tested
    private void getInvoices(Integer uid) throws MalformedURLException, XmlRpcException {
        final XmlRpcClient client = new XmlRpcClient();

        final XmlRpcClient models = new XmlRpcClient() {{
            setConfig(new XmlRpcClientConfigImpl() {{
                setServerURL(new URL(SCHEME,HOST,PORT,"/xmlrpc/object"));
            }});
        }};

        final Object[] invoice_ids = (Object[]) models.execute(
                "execute", asList(
                        DATABASE, uid, PASSWORD, "sale.invoice", "search",
                        asList(asList(asList("type", "=", "out_invoice"), asList("state", "=", "open")))
                ));
        final XmlRpcClientConfigImpl report_config = new XmlRpcClientConfigImpl();
        report_config.setServerURL(new URL(SCHEME,HOST,PORT,"/xmlrpc/object"));
        final Map<String, Object> result = (Map<String, Object>)client.execute(
                report_config, "render_report", asList(
                        DATABASE, uid, PASSWORD,
                        "account.report_invoice",
                        invoice_ids));
        final byte[] report_data = DatatypeConverter.parseBase64Binary((String)result.get("result"));
    }
}
