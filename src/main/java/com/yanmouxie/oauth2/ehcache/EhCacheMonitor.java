package com.yanmouxie.oauth2.ehcache;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * This Class can monitor cache which sync using RMI, even though in different JVM. 
 * It uses same configuration file "ehcache.xml", it will automatically synchronized.
 * The main() method can be run in eclipse,
 * e.g. Eclipse JVM memory cache <--sync--> JBoss JVM memory cache
 * Reference: https://www.ehcache.org/documentation/2.8/replication/rmi-replicated-caching.html
 * @author yanmouxie.com
 *
 */
public class EhCacheMonitor {

	CacheManager manager;
    Cache cache;
    
	public static void main(String[] args) {
		EhCacheMonitor ehCacheMonitor = new EhCacheMonitor();
		ehCacheMonitor.init(args);
		ehCacheMonitor.menu();
	}
	
	private void init(String[] path) {
        if (path.length > 0) {
            manager = CacheManager.newInstance(path[0]);//new CacheManager(fileName);
        } else {
            manager = CacheManager.newInstance(getClass().getResourceAsStream("/ehcache.xml"));//src/main/resources/ehcache.xml
        }
        //System.out.println(manager.getActiveConfigurationText());
        System.out.println("All Cache Name:");
        String names[] = manager.getCacheNames();
        for (String name : names) {
            System.out.println(name);
        }
        
        cache = manager.getCache("accessTokenToRefreshTokenStore");
    }
	
	private void menu() {
        menuItemTop();
    }
	
	private void menuItemTop() {
        Scanner s = new Scanner(System.in);
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("Menu:");
        System.out.println("1,Add cache");
        System.out.println("2,Get cache");
        System.out.println("3,Get cache size");
        System.out.println("4,Cache siez Monitoring (Press T to quit)");
        System.out.println("5,Test hit count");
        System.out.println("C,Claer cache");
        System.out.println("Q,Quit");
        System.out.println("-----------------------------------------------------------------------");
        String str = s.next();
        switch (str) {
            case "1":
                add();
                break;
            case "2":
                get();
                break;
            case "3":
                getAll();
                break;
            case "4":
                monitoring();
                break;
            case "5":
            	testHitCount();
                break;
            case "C":
                claerCache();
                break;
            case "Q":
                closs();
                System.exit(0);
                break;
            default:
                break;
        }
        menu();
    }
	
	private void add() {
        Scanner s = new Scanner(System.in);
        System.out.println("KEY:");
        String key = s.next();
        System.out.println("VALUE:");
        String value = s.next();
        
        /*Element element = cache.get(key);
        if (element == null) {
        	HashSet<OAuth2AccessToken> set = new HashSet<OAuth2AccessToken>();
        	OAuth2AccessToken t = new DefaultOAuth2AccessToken(value);
        	set.add(t);
        	cache.put(new Element(key, set));
        }else
        {
        	HashSet<OAuth2AccessToken> book= (HashSet<OAuth2AccessToken>)element.getObjectValue();
        	OAuth2AccessToken t = new DefaultOAuth2AccessToken(value);
        	book.add(t);
        	cache.put(new Element(key, book));
        }*/
        cache.put(new Element(key, value));
        System.out.println("Success： KEY:" + key + "->value:" + cache.get(key).getObjectValue().toString());
    }
	
	private void get() {
        Scanner s = new Scanner(System.in);
        System.out.println("KEY:");
        String key = s.next();
        Element element = cache.get(key);
        if (element != null) {
            String s1 = element.getObjectValue().toString();
            System.out.println("Success：key:" + key + "->value:" + s1);
            System.out.println("Hit count：" + element.getHitCount());
        }
    }
	
	private void getAll() {
        String names[] = manager.getCacheNames();
        for (String name : names) {
            Cache cache = manager.getCache(name);
            System.out.println( name + " Cache size：" + cache.getSize());
            List<String> list = cache.getKeys();
            if (list.size() > 100) {
                System.out.println("Only output 100 records");
                for (int i = 0; i < 100; i++) {
                	if(cache.get(list.get(i))!=null)
                	{
                		System.out.println(cache.get(list.get(i)).getObjectValue().toString());
                	}
                }
                System.out.println("-----------------------");
            } else {
                for (String s : list) {
                	if(cache.get(s)!=null) {
                		System.out.println("key:" + s + ", value:"+cache.get(s).getObjectValue().toString());
                	}
                }
            }
            System.out.println( " ");
        }
    }
	
	private void monitoring() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                String names[] = manager.getCacheNames();
                for (String name : names) {
                    Cache cache = manager.getCache(name);
                    System.out.println("cache.getSize：" + cache.getSize());
                    System.out.println("cache.getMemoryStoreSize:" + cache.getMemoryStoreSize());
                    System.out.println("cache.getStatistics().cacheHitCount:" + cache.getStatistics().cacheHitCount());
                    System.out.println("cache.getStatistics().cacheMissCount:" + cache.getStatistics().cacheMissCount());
                    System.out.println("-----------------------------------------------------------------------");
                }
            }
        }, 1L, 1000 * 3l);
        while (true) {
            try {
                int str = System.in.read();
                if (str == (int) 'T') {
                    timer.cancel();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
	
	private void testHitCount() {
        Scanner s = new Scanner(System.in);
        System.out.println("Key:");
        String key = s.next();
        for (int i = 0; i < 10000; i++) {
            cache.get(key).getObjectValue().toString();
        }
        System.out.println(cache.get(key).getHitCount());
    }
	
	private void claerCache() {
        System.out.println("Cache siez：" + cache.getSize());
        cache.removeAll();
        cache.flush();
        System.out.println("Cache siez：" + cache.getSize());
    }
	
    private void closs() {
        manager.shutdown();
    }
    
}
