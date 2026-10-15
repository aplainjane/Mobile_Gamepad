param([int]$Seconds = 5, [switch]$ValidateMapping, [switch]$XboxLayout, [string]$SdlPath = 'D:\steam\SDL3.dll')

# Loads Steam's SDL in this process only. Does not edit Steam or install a driver.
if (-not (Test-Path -LiteralPath $SdlPath)) { throw "SDL library not found: $SdlPath" }
$library = $SdlPath.Replace('\', '\\')
$source = @'
using System;
using System.Runtime.InteropServices;
using System.Text;
using System.Diagnostics;
using System.Threading;
public static class PocketSdlProbe {
    const string Lib = "SDL_LIBRARY_PATH";
    [StructLayout(LayoutKind.Sequential)] public struct Guid16 { public ulong A, B; }
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] [return:MarshalAs(UnmanagedType.I1)] static extern bool SDL_Init(uint flags);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern void SDL_Quit();
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern IntPtr SDL_GetError();
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] [return:MarshalAs(UnmanagedType.I1)] static extern bool SDL_SetHint([MarshalAs(UnmanagedType.LPUTF8Str)] string name, [MarshalAs(UnmanagedType.LPUTF8Str)] string value);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern IntPtr SDL_GetJoysticks(out int count);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern void SDL_free(IntPtr pointer);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern IntPtr SDL_GetJoystickNameForID(uint id);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern ushort SDL_GetJoystickVendorForID(uint id);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern ushort SDL_GetJoystickProductForID(uint id);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern Guid16 SDL_GetJoystickGUIDForID(uint id);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern void SDL_GUIDToString(Guid16 guid, StringBuilder text, int size);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] [return:MarshalAs(UnmanagedType.I1)] static extern bool SDL_IsGamepad(uint id);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern IntPtr SDL_GetGamepadMappingForID(uint id);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern int SDL_GetGamepadTypeForID(uint id);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern int SDL_GetRealGamepadTypeForID(uint id);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern IntPtr SDL_GetGamepadStringForType(int type);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern IntPtr SDL_OpenJoystick(uint id);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern void SDL_CloseJoystick(IntPtr joystick);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern int SDL_GetNumJoystickAxes(IntPtr joystick);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern int SDL_GetNumJoystickButtons(IntPtr joystick);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern int SDL_GetNumJoystickHats(IntPtr joystick);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern short SDL_GetJoystickAxis(IntPtr joystick, int axis);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] [return:MarshalAs(UnmanagedType.I1)] static extern bool SDL_GetJoystickButton(IntPtr joystick, int button);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern byte SDL_GetJoystickHat(IntPtr joystick, int hat);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern void SDL_UpdateJoysticks();
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern int SDL_AddGamepadMapping([MarshalAs(UnmanagedType.LPUTF8Str)] string mapping);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern IntPtr SDL_OpenGamepad(uint id);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern short SDL_GetGamepadAxis(IntPtr gamepad, int axis);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] [return:MarshalAs(UnmanagedType.I1)] static extern bool SDL_GetGamepadButton(IntPtr gamepad, int button);
    [DllImport(Lib, CallingConvention=CallingConvention.Cdecl)] static extern void SDL_CloseGamepad(IntPtr gamepad);
    static string Text(IntPtr p) { return p == IntPtr.Zero ? "(none)" : Marshal.PtrToStringUTF8(p); }
    public static string Mapping(string guid) {
        return guid + ",Pocket Gamepad,a:b0,b:b1,x:b2,y:b3,leftshoulder:b4,rightshoulder:b5,back:b8,start:b9,leftstick:b10,rightstick:b11,guide:b16,leftx:a0,lefty:a1,rightx:a2,righty:a3,lefttrigger:a4,righttrigger:a5,dpup:h0.1,dpright:h0.2,dpdown:h0.4,dpleft:h0.8,platform:Windows,";
    }
    public static void Run(int seconds, bool validate, bool xboxLayout) {
        SDL_SetHint("SDL_JOYSTICK_ALLOW_BACKGROUND_EVENTS", "1");
        if (!SDL_Init(0x200 | 0x2000)) throw new Exception(Text(SDL_GetError()));
        try {
            int count; var ids = SDL_GetJoysticks(out count);
            try {
                Console.WriteLine("SDL joystick count: " + count);
                for (int n=0; n<count; n++) {
                    uint id = unchecked((uint)Marshal.ReadInt32(ids,n*4));
                    ushort vid=SDL_GetJoystickVendorForID(id), pid=SDL_GetJoystickProductForID(id);
                    var guid = new StringBuilder(33); SDL_GUIDToString(SDL_GetJoystickGUIDForID(id),guid,33);
                    Console.WriteLine("Device: {0} VID/PID={1:X4}/{2:X4} GUID={3} IsGamepad={4}", Text(SDL_GetJoystickNameForID(id)),vid,pid,guid,SDL_IsGamepad(id));
                    if (vid!=0x00e0 || pid!=0) continue;
                    var existing=SDL_GetGamepadMappingForID(id);
                    Console.WriteLine("Existing mapping: " + Text(existing)); SDL_free(existing);
                    var joystick = SDL_OpenJoystick(id);
                    if (joystick==IntPtr.Zero) throw new Exception(Text(SDL_GetError()));
                    IntPtr gamepad = IntPtr.Zero;
                    try {
                        int axes=SDL_GetNumJoystickAxes(joystick), buttons=SDL_GetNumJoystickButtons(joystick), hats=SDL_GetNumJoystickHats(joystick);
                        Console.WriteLine("Raw controls: axes={0} buttons={1} hats={2}",axes,buttons,hats);
                        if (validate) {
                            string mapping=Mapping(guid.ToString());
                            if (xboxLayout) mapping += "type:xboxone,";
                            Console.WriteLine("Candidate mapping: " + mapping);
                            int result=SDL_AddGamepadMapping(mapping);
                            Console.WriteLine("Add mapping result={0}, IsGamepad={1}",result,SDL_IsGamepad(id));
                            Console.WriteLine("Mapped type={0}; real device type={1}",Text(SDL_GetGamepadStringForType(SDL_GetGamepadTypeForID(id))),Text(SDL_GetGamepadStringForType(SDL_GetRealGamepadTypeForID(id))));
                            gamepad=SDL_OpenGamepad(id);
                            if (gamepad==IntPtr.Zero) throw new Exception(Text(SDL_GetError()));
                        }
                        var timer=Stopwatch.StartNew(); string previous=null; int changes=0;
                        while(timer.Elapsed.TotalSeconds<seconds) {
                            SDL_UpdateJoysticks(); var state=new StringBuilder();
                            state.Append("axes="); for(int a=0;a<axes;a++) state.Append(SDL_GetJoystickAxis(joystick,a)).Append(',');
                            state.Append(" buttons="); for(int b=0;b<buttons;b++) if(SDL_GetJoystickButton(joystick,b)) state.Append(b).Append(',');
                            state.Append(" hats="); for(int h=0;h<hats;h++) state.Append(SDL_GetJoystickHat(joystick,h)).Append(',');
                            if(gamepad!=IntPtr.Zero) {
                                state.Append(" mapped(LX,LY,RX,RY,LT,RT)="); for(int a=0;a<6;a++) state.Append(SDL_GetGamepadAxis(gamepad,a)).Append(',');
                                state.Append(" mappedButtons="); for(int b=0;b<15;b++) if(SDL_GetGamepadButton(gamepad,b)) state.Append(b).Append(',');
                            }
                            string current=state.ToString(); if(current!=previous) { if(previous!=null) changes++; Console.WriteLine("{0:F1}s {1}",timer.Elapsed.TotalSeconds,current); previous=current; }
                            Thread.Sleep(25);
                        }
                        Console.WriteLine("Input changes: " + changes);
                    } finally { if(gamepad!=IntPtr.Zero) SDL_CloseGamepad(gamepad); SDL_CloseJoystick(joystick); }
                }
            } finally { SDL_free(ids); }
        } finally { SDL_Quit(); }
    }
}
'@
Add-Type -TypeDefinition ($source.Replace('SDL_LIBRARY_PATH', $library))
[PocketSdlProbe]::Run([Math]::Clamp($Seconds, 1, 60), ($ValidateMapping.IsPresent -or $XboxLayout.IsPresent), $XboxLayout.IsPresent)
