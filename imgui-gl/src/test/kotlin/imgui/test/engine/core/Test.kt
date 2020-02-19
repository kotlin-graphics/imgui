package imgui.test.engine.core

import imgui.test.engine.context.TestContext

//-------------------------------------------------------------------------
// ImGuiTest
//-------------------------------------------------------------------------

typealias TestRunFunc = (ctx: TestContext) -> Unit
typealias TestGuiFunc = (ctx: TestContext) -> Unit
typealias TestTestFunc = (ctx: TestContext) -> Unit

// Wraps a placement new of a given type (where 'buffer' is the allocated memory)
//typedef void    (*ImGuiTestUserDataConstructor)(void* buffer);
//typedef void    (*ImGuiTestUserDataDestructor)(void* ptr);

// Storage for one test
class Test {

    var group = TestGroup.Unknown              // Coarse groups: 'Tests' or 'Perf'
    var nameOwned = false          //
    var category: String? = null           // Literal, not owned
    var name: String? = null               // Literal, generally not owned unless NameOwned=true
    var sourceFile: String? = null         // __FILE__
    var sourceFileShort: String? = null    // Pointer within SourceFile, skips filename.
    var sourceLine = 0         // __LINE__
    var sourceLineEnd = 0      //
    var argVariant = 0         // User parameter, for use by GuiFunc/TestFunc. Generally we use it to run variations of a same test.
    var userDataSize = 0       // When SetUserDataType() is used, we create an instance of user structure so we can be used by GuiFunc/TestFunc.

    //    ImGuiTestUserDataConstructor    UserDataConstructor
//    ImGuiTestUserDataDestructor     UserDataDestructor
    var status = TestStatus.Unknown
    var flags = TestFlag.None.i              // See ImGuiTestFlags_
    var guiFunc: TestGuiFunc? = null            // GUI functions can be reused
    var testFunc: TestTestFunc? = null           // Test function
    val testLog = TestLog()

    fun setOwnedName(name: String)    {
        assert(!nameOwned)
        nameOwned = true
        this.name = name
    }

//    template <typename T>
//    void SetUserDataType()
//    {
//        UserDataSize = sizeof(T)
//        UserDataConstructor = [](void * ptr) { IM_PLACEMENT_NEW(ptr) T; }
//        UserDataDestructor = [](void * ptr) { IM_UNUSED(ptr); reinterpret_cast < T * >(ptr)->~T(); }
//    }
}