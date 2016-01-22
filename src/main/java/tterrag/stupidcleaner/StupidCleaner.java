package tterrag.stupidcleaner;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.SneakyThrows;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.FMLRelaunchLog;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LoggerContext;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;


@Mod(modid = "stupidcleaner", name = "StupidCleaner", version = "@VERSION@", dependencies = "before:*")
public class StupidCleaner {
    
    static final Logger logger = LogManager.getLogger("StupidCleaner");
    static final StupidFilter filter;
    
    private static class FilteredPrintStream extends PrintStream {

        private Set<String> filtered = Sets.newHashSet();
        
        public FilteredPrintStream(OutputStream out) {
            super(out);
        }

        @Override
        public void print(Object obj) {
            if (allow(String.valueOf(obj))) {
                super.print(obj);
            }
        }

        @Override
        public void print(String s) {
            if (allow(s)) {
                super.print(s);
            }
        }
        
        @Override
        public void println(Object x) {
            if (allow(String.valueOf(x))) {
                super.println(x);
            }
        }
        
        @Override
        public void println(String x) {
            if (allow(x)) {
                super.println(x);
            }
        }

        private boolean allow(String s) {
            if (filtered.contains(s)) {
                return false;
            } else if (filter.filter(s) == Result.DENY) {
                filtered.add(s);
                return false;
            }
            return true;
        }
    }
    
    static {
        logger.info("Injecting logger config");

        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.addFilter(filter = StupidFilter.createFilter());
        
        ((org.apache.logging.log4j.core.Logger)FMLRelaunchLog.log.getLogger()).getContext().addFilter(filter);

        System.setOut(new FilteredPrintStream(System.out));
        System.setErr(new FilteredPrintStream(System.err));
        
        logger.info("lel", new NullPointerException());
    }
    
    @Instance("stupidcleaner")
    @Getter
    private static StupidCleaner instance;
    

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SuppressWarnings("unchecked")
    @SideOnly(Side.CLIENT)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    @SneakyThrows
    public void onModelBakePost(ModelBakeEvent event) {
        Map<ResourceLocation, Exception> modelErrors = (Map<ResourceLocation, Exception>) ReflectionHelper.getPrivateValue(ModelLoader.class, event.modelLoader, "loadingExceptions");
        Set<ModelResourceLocation> missingVariants = (Set<ModelResourceLocation>) ReflectionHelper.getPrivateValue(ModelLoader.class, event.modelLoader, "missingVariants");
        Multiset<String> suppressed = HashMultiset.create();
        
        suppressed.addAll(modelErrors.keySet().stream().map(r -> r.getResourceDomain()).collect(Collectors.toList()));
        suppressed.addAll(missingVariants.stream().map(r -> r.getResourceDomain()).collect(Collectors.toList()));
        
        suppressed.entrySet().forEach(e -> logger.error("There were {} model errors for domain {}. Suppressing...", e.getCount(), e.getElement()));
        
        modelErrors.clear();
        missingVariants.clear();
    }
}
