package ctbrec.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

public class CookieJarImpl implements CookieJar {

    private static final transient Logger LOG = LoggerFactory.getLogger(CookieJarImpl.class);

    private final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        String host = getHost(url);
        List<Cookie> cookiesForUrl = cookieStore.get(host);
        if (cookiesForUrl != null) {
            cookiesForUrl = new ArrayList<Cookie>(cookiesForUrl); //unmodifiable
            for (Iterator<Cookie> iterator = cookiesForUrl.iterator(); iterator.hasNext();) {
                Cookie oldCookie = iterator.next();
                String name = oldCookie.name();
                for (Cookie newCookie : cookies) {
                    if(newCookie.name().equalsIgnoreCase(name)) {
                        LOG.debug("Replacing cookie {} {} -> {} [{}]", oldCookie.name(), oldCookie.value(), newCookie.value(), oldCookie.domain());
                        iterator.remove();
                        break;
                    }
                }
            }
            cookiesForUrl.addAll(cookies);
            cookieStore.put(host, cookiesForUrl);
            LOG.debug("Adding cookie: {} for {}", cookiesForUrl, host);
        }
        else {
            cookieStore.put(host, cookies);
            LOG.debug("Storing cookie: {} for {}", cookies, host);
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        String host = getHost(url);
        List<Cookie> cookies = cookieStore.get(host);
        LOG.debug("Cookies for {}", url);
        Optional.ofNullable(cookies).ifPresent(cookiez -> cookiez.forEach(c -> {
            LOG.debug("  {} expires on:{}", c, c.expiresAt());
        }));
        //LOG.debug("Cookies for {}: {}", url.host(), cookies);
        return cookies != null ? cookies : new ArrayList<Cookie>();
    }

    public Cookie getCookie(HttpUrl url, String name) {
        List<Cookie> cookies = loadForRequest(url);
        for (Cookie cookie : cookies) {
            if(Objects.equals(cookie.name(), name)) {
                return cookie;
            }
        }
        throw new NoSuchElementException("No cookie named " + name + " for " + url.host() + " available");
    }

    private String getHost(HttpUrl url) {
        String host = url.host();
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }
        return host;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, List<Cookie>> entry : cookieStore.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        return sb.toString();
    }

    public Map<String, List<Cookie>> getCookies() {
        return cookieStore;
    }

    public void clear() {
        cookieStore.clear();
    }
}
