package com.mahitotsu.tsukumogami.apl.tools.provisioner;

import java.util.Map;

import com.mahitotsu.tsukumogami.apl.tools.ActionGroupAttributes;
import com.mahitotsu.tsukumogami.apl.tools.FunctionAttributes;
import com.mahitotsu.tsukumogami.apl.tools.ParameterAttributes;

@ActionGroupAttributes(name = "ActionGroupProvisioner", description = """
        エージェントが利用可能なツールをActionGroupとして利用できるように有効化する操作を提供します。
        """)
public interface ActionGroupProvisioner {

    @FunctionAttributes(name = "activateActionGroups", description = """
            指定された名称のActionGroupを利用できるように有効化します。
            指定された名称の中で有効化できないツールがあった場合、エラーが返ります。
            有効化に成功した場合は有効化されたActionGroupの名称の配列をJSON文字列で返します。
            """)
    String[] activateActionGroups(
        @ParameterAttributes(name="actionGroupNames", description = """
                有効化するActionGroupの名前。複数指定可能です。
                """, required = true, type = "array")
        String... actionGroupNames
    );

    @FunctionAttributes(name = "listAvailableActionGroups", description = """
            利用可能なツールの名称を含む配列を返します。返されたツールは有効化してエージェントから利用可能です。
            返される配列の要素はActionGroupの名称と説明です。
            利用可能なActionGroupがない場合は空の配列が返ります。
            """)
    Map<String, String>[] listAvailableActionGroups();
}
