package rpc.servlet;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.ReadListener;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RPCServletTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void doGet() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(response.getWriter()).thenReturn(printWriter);

        new RPCServlet().doGet(request, response);

        JSONRPC2Error error = new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), "You need to Post JSON-RPC request!");
        assertEquals(error.toString(), stringWriter.toString());
    }

    //@Test
    public void doPostWithName() throws Exception {

        String method = "Calculator.multiplier";
        Map<String, Object> params = new HashMap<>();
        params.put("a", 1);
        params.put("b", 2);
        String id = "1";
        JSONRPC2Request req = new JSONRPC2Request(method, params, id);

        String json = req.toJSONString();
        when(request.getInputStream()).thenReturn(createServletInputStream(json, "UTF-8"));
        when(request.getContentType()).thenReturn("application/json");
        when(request.getCharacterEncoding()).thenReturn("UTF-8");

        ServletContext ctx = mock(ServletContext.class);
        when(ctx.getServletContextName()).thenReturn("mock");
        when(ctx.getRealPath("/WEB-INF/classes/rpc/service/Calculator.class")).thenReturn("TODO");

        ServletConfig cfg = mock(ServletConfig.class);
        when(cfg.getServletContext()).thenReturn(ctx);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(response.getWriter()).thenReturn(printWriter);

        RPCServlet servlet = new RPCServlet();
        servlet.init(cfg);
        servlet.doPost(request, response);

        JSONRPC2Response jsonResponse = new JSONRPC2Response(id);
        jsonResponse.setResult(3);
        assertEquals(jsonResponse.toString(), stringWriter.toString());

    }

    ServletInputStream createServletInputStream(String s, String charset) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            baos.write(s.getBytes(charset));
        } catch (Exception e) {
            throw new RuntimeException("No support charset.");
        }

        final InputStream bais = new ByteArrayInputStream(baos.toByteArray());

        return new ServletInputStream() {

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            @Override
            public int read() throws IOException {
                return bais.read();
            }
        };
    }


}
