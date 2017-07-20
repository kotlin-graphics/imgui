package imgui.imgui

import imgui.ImGui.currentWindow


/** ID scopes
 *  If you are creating widgets in a loop you most likely want to push a unique identifier so ImGui can differentiate
 *  them.
 *  You can also use the "##foobar" syntax within widget label to distinguish them from each others. Read "A primer on
 *  the use of labels/IDs" in the FAQ for more details. **/
interface imgui_id {

    /** push identifier into the ID stack. IDs are hash of the *entire* stack!  */
    fun pushId(strId:String) = currentWindow.run { idStack.add(getId(strId)) }

//    IMGUI_API void          PushID(const char* str_id_begin, const char* str_id_end);
//    IMGUI_API void          PushID(const void* ptr_id);

    fun pushId(id: Int) = currentWindow.idStack.push(id)

    fun popId() =  currentWindow.idStack.pop()

//    IMGUI_API ImGuiID       GetID(const char* str_id);                                          // calculate unique ID (hash of whole ID stack + given parameter). useful if you want to query into ImGuiStorage yourself. otherwise rarely needed
//    IMGUI_API ImGuiID       GetID(const char* str_id_begin, const char* str_id_end);
//    IMGUI_API ImGuiID       GetID(const void* ptr_id);
}