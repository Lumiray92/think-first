package com.thinkfirst.auth;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkfirst.common.ApiResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
/** 敏感接口按来源限制频率。只信任直连地址，不接受客户端伪造的转发头。 */
@Component
public class SensitiveRequestFilter extends OncePerRequestFilter {
    private record Window(long start,int count) {}
    private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    public SensitiveRequestFilter(ObjectMapper mapper){this.mapper=mapper;}
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        response.setHeader("Cache-Control","no-store");
        response.setHeader("Pragma","no-cache");
        String path=request.getRequestURI();
        if (!"OPTIONS".equals(request.getMethod()) && (path.endsWith("/auth/login") || path.endsWith("/auth/register") || (path.contains("/user/api-key") && !"GET".equals(request.getMethod())))) {
            long now=System.currentTimeMillis();
            windows.entrySet().removeIf(entry->now-entry.getValue().start()>60000);
            String key=request.getRemoteAddr()+":"+path;
            // 限制桶数量，防止来源地址过多导致内存无限增长。
            if(windows.size()>10000 && !windows.containsKey(key)){reject(response);return;}
            Window window=windows.compute(key,(ignored,old)->old==null || now-old.start()>60000?new Window(now,1):new Window(old.start(),old.count()+1));
            if(window.count()>10){reject(response);return;}
        }
        chain.doFilter(request,response);
    }
    private void reject(HttpServletResponse response)throws IOException {
        response.setStatus(429);response.setHeader("Retry-After","60");response.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(response.getWriter(),ApiResponse.error(429,"操作过于频繁，请一分钟后再试"));
    }
}
