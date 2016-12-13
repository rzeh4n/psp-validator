package nkp.pspValidator.gui;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;
import javafx.application.Application;

import java.util.logging.Logger;

/**
 * Created by martin on 13.12.16.
 */
public abstract class AbstractController extends Application {

    private static Logger LOG = Logger.getLogger(AbstractController.class.getSimpleName());

    protected Main main;

    public void setMain(Main main) {
        LOG.info("setMain");
        this.main = main;
    }

    protected void openUrl(String url) {
        HostServicesDelegate hostServices = HostServicesFactory.getInstance(this);
        getHostServices().showDocument(url);
    }

}
