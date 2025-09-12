package iFogSim;

import java.util.HashMap;
import org.apache.commons.math3.util.Pair;
import org.fog.application.AppModule;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.FogUtils;

public class ContextGeneratorModule extends AppModule {
    public ContextGeneratorModule(String name, String appId, int userId) {
        super(
                FogUtils.generateEntityId(),
                name,
                appId,
                userId,
                10.0,
                10,
                1000L,
                1000L,
                "VMM",
                new TupleScheduler(10.0, 1),
                new HashMap<>()
        );
    }
}