package tterrag.stupidcleaner;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.SneakyThrows;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StringFormattedMessage;

import com.google.common.collect.Lists;

@Plugin(name = "StupidFilter", category = "Core", elementType = "filter", printObject = true)
public final class StupidFilter extends AbstractFilter {
    
    private List<Pattern> regexFilters = Lists.newArrayList();

    private StupidFilter() throws IOException {
        super(Result.DENY, Result.NEUTRAL);
        File config = new File("config/stupidfilter.txt");
        System.out.println("Creating/reading config file at: " + config.getAbsolutePath());
        config.createNewFile();
        List<String> filters = FileUtils.readLines(config);
        System.out.println("Read lines: " + filters);
        
        for (String s : filters) {
            regexFilters.add(Pattern.compile(s));
        }
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
        if (msg != null) {
            return filter(new StringFormattedMessage(msg, params).getFormattedMessage());
        }
        return onMismatch;
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
        if (msg == null) {
            return onMismatch;
        }
        Result msgRes = filter(msg.toString());
        Result throwableRes = filter(t);
        return throwableRes == onMatch ? throwableRes : msgRes;
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
        if (msg == null) {
            return onMismatch;
        }
        Result msgRes = filter(msg.getFormattedMessage());
        Result throwableRes = filter(t);
        return throwableRes == onMatch ? throwableRes : msgRes;
    }

    @Override
    public Result filter(final LogEvent event) {
        return filter(event.getMessage().getFormattedMessage());
    }

    Result filter(Throwable t) {
        List<String> stacktrace = Lists.newArrayList();
        t.printStackTrace(new PrintStream(System.err) {
            @Override
            public void println(Object o) {
                stacktrace.add(String.valueOf(o));
            }
        });
        for (String s : stacktrace) {
            if (filter(s) == onMatch) {
                return onMatch;
            }
        }
        return onMismatch;
    }
    
    Result filter(final String msg) {
        if (msg == null) {
            return onMismatch;
        }
        for (Pattern p : regexFilters) {
            final Matcher m = p.matcher(msg);
            if (m.find()) {
                return onMatch;
            }
        }
        return onMismatch;
    }

    @PluginFactory
    @SneakyThrows
    public static StupidFilter createFilter() {
        System.out.println("Creating filter!");
        return new StupidFilter();
    }
}
