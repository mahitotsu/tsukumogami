package com.mahitotsu.tsukumogami.apl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import com.mahitotsu.tsukumogami.apl.TestBase;

public class SimpleInlineAgentServiceTest extends TestBase {

    @Autowired
    private SimpleInlineAgentService inlineAgentService;

    @Test
    @WithMockUser(username = "usr001@test.local", roles = "standard")
    public void testAuthorizedStandardUser() {

        System.out.println("----------");
        final String response = this.inlineAgentService.execute("""
                今日から10000日後の日付を教えてください。
                結果は必ず'yyyy-mm-dd'のフォーマットで、計算結果以外の文字は一切含めないでください。
                """);

        assertEquals(LocalDate.now().plusDays(10000).format(DateTimeFormatter.ISO_DATE), response);
    }

    @Test
    @WithMockUser(username = "usr002@test.local", roles = "premium")
    public void testAuthorizedPremiumUser() {

        System.out.println("----------");
        final String response = this.inlineAgentService.execute("""
                今日から10000日後の日付を教えてください。
                結果は必ず'yyyy-mm-dd'のフォーマットで、計算結果以外の文字は一切含めないでください。
                """);

        assertEquals(LocalDate.now().plusDays(10000).format(DateTimeFormatter.ISO_DATE), response);
    }

    @Test
    public void testUnauthorized() {

        System.out.println("----------");
        final String response = this.inlineAgentService.execute("""
                今日から10000日後の日付を教えてください。
                結果は必ず'yyyy-mm-dd'のフォーマットで、計算結果以外の文字は一切含めないでください。
                """);

        assertNotEquals(LocalDate.now().plusDays(10000).format(DateTimeFormatter.ISO_DATE), response);
    }
}
