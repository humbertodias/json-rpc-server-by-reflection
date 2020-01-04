package rpc.servlet;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParseException;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@WebServlet(urlPatterns = {"/rpc"}, loadOnStartup = 1)
public class RPCServlet extends HttpServlet {

    private final String SERVICE_PACKAGE = "rpc.service.";

    static class Strings {

        // Read Post data from the request
        public static String toString(InputStream inputStream) throws IOException {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                return br.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }

        // display backtrace of the exception
        public static String backTrace(Throwable e) {
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append(element.toString()).append("\n");
            }
            return sb.toString();
        }

    }

    // load Service class dynamicaly
    public Object loadService(String className) throws Exception {
        ClassLoader parentClassLoader = ServiceReloader.class.getClassLoader();
        ServiceReloader classLoader = new ServiceReloader(parentClassLoader, this.getServletContext());
        Class<?> clazz = classLoader.loadClass(className);
        return clazz.getConstructor().newInstance();
    }

    public JSONRPC2Response internalError(Object request_id, String message) {
        return internalError(request_id, message, null);
    }

    public JSONRPC2Response internalError(Object requestId, String message, String backtrace) {
        Map<String, String> data = new HashMap<>();
        data.put("exception", message);
        if (backtrace != null) {
            data.put("backtrace", backtrace);
        }
        JSONRPC2Error error = new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), "Internal Server Error", data);
        return new JSONRPC2Response(error, requestId);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), "You need to Post JSON-RPC request!"));
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Object id = null;
        JSONRPC2Response jsonResponse = null;
        try {
            JSONRPC2Request jsonRequest = JSONRPC2Request.parse(Strings.toString(request.getInputStream()));
            id = jsonRequest.getID();

            Object[] params = parameters(jsonRequest);
            Class<?>[] paramTypes = parameterTypes(params);

            String[] partsName = jsonRequest.getMethod().split("\\.");
            String className = SERVICE_PACKAGE + partsName[0];
            String methodName = partsName[1];

            Object service = loadService(className);
            Method method = service.getClass().getMethod(methodName, paramTypes);
            Object result = method.invoke(service, params);

            jsonResponse = new JSONRPC2Response(result, id);
        } catch (IllegalArgumentException e) {
            jsonResponse = new JSONRPC2Response(JSONRPC2Error.INVALID_PARAMS, id);
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
            jsonResponse = internalError(id, e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            String message = cause.getClass().getName() + ": " + cause.getMessage();
            jsonResponse = internalError(id, message, Strings.backTrace(cause));
        } catch (JSONRPC2ParseException e) {
            jsonResponse = new JSONRPC2Response(JSONRPC2Error.PARSE_ERROR, id);
        } catch (NoSuchMethodException e) {
            jsonResponse = new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, id);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            String message = cause != null ? cause.getMessage() : null;
            jsonResponse = internalError(id, message, Strings.backTrace(e));
        }
        response.getWriter().print(jsonResponse);
    }

    private Class<?>[] parameterTypes(Object[] params) {
        Class<?>[] types = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            types[i] = params[i].getClass();
        }
        return types;
    }

    private Object[] parameters(JSONRPC2Request request) {
        Object[] params = new Object[0];
        if (request.getNamedParams() != null) {
            params = request.getNamedParams().values().toArray();
        }
        if (request.getPositionalParams() != null) {
            params = request.getPositionalParams().toArray();
        }
        return params;
    }

    // the only way to reload a class on runtime
    class ServiceReloader extends ClassLoader {
        private ServletContext servletContext;

        public ServiceReloader(ClassLoader parent, ServletContext servletContext) {
            super(parent);
            this.servletContext = servletContext;
        }

        private byte[] loadClassFromFile(String fileName) throws IOException {
            FileInputStream inputStream = new FileInputStream(fileName);
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            int nextValue = 0;
            while ((nextValue = inputStream.read()) != -1) {
                byteStream.write(nextValue);
            }
            return byteStream.toByteArray();
        }

        public Class loadClass(String name) throws ClassNotFoundException {
            if (!name.startsWith(SERVICE_PACKAGE)) {
                return super.loadClass(name);
            }
            try {
                String path = getWARClassPath(name);
                byte[] classData = loadClassFromFile(path);
                return defineClass(name, classData, 0, classData.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(e.getMessage());
            }
        }

        // convert class name to path
        public String getWARClassPath(String name) {
            String classPath = String.join(File.separator, name.split("\\."));
            return servletContext.getRealPath("/WEB-INF/classes/" + classPath + ".class");
        }

    }

}

