package com.mahitotsu.tsukumogami.apl.tools.calendar;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.springframework.stereotype.Component;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupProperties;

@Component
public class CalendarToolBean extends ActionGroupProperties implements CalendarTool {

    public CalendarToolBean() {
        super("CalendarTool", """
                日付に関する機能を提供します。
                """, Arrays.asList(
                new FunctionProperties("now", """
                        現在日時を返します。
                        """,
                        null)),
                CalendarTool.class);
    }

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }

}
