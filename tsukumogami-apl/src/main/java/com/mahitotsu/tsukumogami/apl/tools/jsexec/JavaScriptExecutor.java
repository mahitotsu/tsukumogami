package com.mahitotsu.tsukumogami.apl.tools.jsexec;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupAttributes;
import com.mahitotsu.tsukumogami.apl.tools.FunctionAttributes;
import com.mahitotsu.tsukumogami.apl.tools.ParameterAttributes;

@ActionGroupAttributes(name = "JavaScriptExecutor", description = """
        JavaScriptのコードを実行して結果を返すツールです。
        外部ライブラリが不要なjavascriptのコードをサポートしています。

        ----
        例えば、以下のような処理が実行可能です。
        * 算術計算、日付計算、乱数の生成
        * 現在日付、現在時刻、現在日時の取得
        * 文字列分割、文字列結合、などの文字列処理
         """)
public interface JavaScriptExecutor {

    @FunctionAttributes(name = "execute", description = """
            指定されたJavaScriptのコードを実行して結果を返します。
            """)
    public Object execute(
            @ParameterAttributes(name = "script", description = """
                    実行されるJavaScriptのコードを指定します。
                    """, required = true, type = "string") String script);
}
