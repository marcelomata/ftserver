package ftserver;

import ftserver.fts.Engine;
import iBoxDB.LocalServer.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import static java.lang.System.in;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/*
Turn off virtual memory for 8G+ RAM Machine
use DatabaseConfig.CacheLength and PageText.max_text_length to Control Memory

Linux:
 # free -h
 # sudo swapoff -a
 # free -h 

Windows:
System Properties(Win+Pause) - Advanced system settings - Advanced
- Performance Settings - Advanced - Virtual Memory Change -
uncheck Automatically manage paging file - select No paging file - 
click Set - OK restart
 */
@WebListener
public class AppListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        //Path
        String path = System.getProperty("user.home") + File.separatorChar + "ftsdata130" + File.separatorChar;
        new File(path).mkdirs();

        if (!new File(path).exists()) {

            String tmpPath = sce.getServletContext().getRealPath("/")
                    + "WEB-INF" + File.separatorChar + "DB" + File.separatorChar;

            path = tmpPath;
            (new File(path)).mkdirs();
        }

        Logger.getLogger(App.class.getName()).log(Level.INFO,
                System.getProperty("java.version"));
        Logger.getLogger(App.class.getName()).log(Level.INFO,
                String.format("DB Path=%s ", path));

        DB.root(path);

        //get more os memory
        /*
        Logger.getLogger(AppListener.class.getName()).log(Level.INFO, "Loading Memory...");
        for (File file : new File(path).listFiles()) {
            RandomAccessFile rf;
            try {
                rf = new RandomAccessFile(file, "r");
                byte[] bs = new byte[1024 * 1024 * 32];
                while (rf.read(bs) > 0) {

                }
                rf.close();
                Logger.getLogger(AppListener.class.getName()).log(Level.INFO, file.getAbsolutePath());
            } catch (Throwable ex) {
                Logger.getLogger(AppListener.class.getName()).log(Level.INFO, null, ex);
            }
        }
         */
        //Config
        DB db = new DB(1);
        DatabaseConfig cfg = db.getConfig();
        long tm = java.lang.Runtime.getRuntime().maxMemory();

        cfg.CacheLength = tm / 3;
        //if update the metadata, set low cache
        //cfg.CacheLength = cfg.mb(128);

        cfg.FileIncSize = (int) cfg.mb(4);
        cfg.SwapFileBuffer = (int) cfg.mb(4);
        Logger.getLogger(App.class.getName()).log(Level.INFO, "DB Cache=" + cfg.CacheLength / 1024 / 1024 + "MB"
                + " AppMEM=" + tm / 1024 / 1024 + "MB");

        new Engine().Config(cfg);

        cfg.ensureTable(Page.class, "Page", "url(" + Page.MAX_URL_LENGTH + ")");
        cfg.ensureIndex(Page.class, "Page", true, "textOrder");

        cfg.ensureTable(PageText.class, "PageText", "id");
        cfg.ensureIndex(PageText.class, "PageText", false, "textOrder");
        cfg.ensureTable(PageSearchTerm.class, "/PageSearchTerm", "time", "keywords(" + PageSearchTerm.MAX_TERM_LENGTH + ")", "uid");

        App.Auto = db.open();

        Logger.getLogger(App.class.getName()).log(Level.INFO, "DB Started...");

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        IndexPage.shutdown();
        if (App.Auto != null) {
            App.Auto.getDatabase().close();
        }
        App.Auto = null;
        Logger.getLogger(App.class.getName()).log(Level.INFO, "DB Closed");
    }
}