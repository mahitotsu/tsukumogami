package com.mahitotsu.tsukumogami.apl.tools.calendar;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupProperties;

@Component
public class CalendarToolBean extends ActionGroupProperties implements CalendarTool {

    public CalendarToolBean() {
        super(CalendarTool.class);
    }

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }

}
