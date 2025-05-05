package com.mahitotsu.tsukumogami.apl.tools.calendar;

import java.time.LocalDateTime;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupAttributes;
import com.mahitotsu.tsukumogami.apl.tools.FunctionAttributes;

@ActionGroupAttributes(name="CalendarTool", description = """
        日付に関する機能を提供します。
        """)
public interface CalendarTool {
    
    @FunctionAttributes(name="now", description = """
            現在日時を返します。
            """)
    LocalDateTime now();
}