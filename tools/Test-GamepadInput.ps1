param([int]$Seconds = 15)

# Read-only WinMM probe: does not create a virtual controller or change settings.
Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
using System.Threading;
using System.Diagnostics;
public static class PocketPadProbe {
    [StructLayout(LayoutKind.Sequential, CharSet=CharSet.Unicode)]
    public struct Caps {
        public ushort Mid, Pid;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst=32)] public string Name;
        public uint Xmin, Xmax, Ymin, Ymax, Zmin, Zmax, Buttons, PeriodMin, PeriodMax;
        public uint Rmin, Rmax, Umin, Umax, Vmin, Vmax, Features, MaxAxes, Axes, MaxButtons;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst=32)] public string RegKey;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst=260)] public string Oem;
    }
    [StructLayout(LayoutKind.Sequential)]
    public struct Pos {
        public uint Size, Flags, X, Y, Z, R, U, V, Buttons, ButtonNumber, Pov, Reserved1, Reserved2;
    }
    [DllImport("winmm.dll")] public static extern uint joyGetNumDevs();
    [DllImport("winmm.dll", EntryPoint="joyGetDevCapsW", CharSet=CharSet.Unicode)]
    public static extern uint joyGetDevCaps(UIntPtr id, out Caps caps, uint size);
    [DllImport("winmm.dll")] public static extern uint joyGetPosEx(uint id, ref Pos pos);
    public static void Monitor(int seconds) {
        int found=0;
        for (uint id=0; id<joyGetNumDevs(); id++) {
            Caps c;
            if (joyGetDevCaps(new UIntPtr(id), out c, (uint)Marshal.SizeOf(typeof(Caps))) != 0) continue;
            if (c.Mid!=0x00E0 || c.Pid!=0) continue;
            found++;
            Console.WriteLine("Android HID candidate {0}: axes={1} buttons={2}",id,c.Axes,c.Buttons);
            var timer=Stopwatch.StartNew();
            string previous=null;
            int changes=0;
            uint lastError=0;
            while(timer.Elapsed.TotalSeconds<seconds) {
                var p=new Pos { Size=(uint)Marshal.SizeOf(typeof(Pos)), Flags=255 };
                var result=joyGetPosEx(id,ref p);
                if (result!=0) {
                    if (lastError!=result) Console.WriteLine("Read failed: {0}",result);
                    lastError=result;
                } else {
                    lastError=0;
                    string state=String.Format("X={0} Y={1} Z={2} R={3} U={4} V={5} buttons={6:X} POV={7}",
                        p.X,p.Y,p.Z,p.R,p.U,p.V,p.Buttons,p.Pov);
                    if(state!=previous) {
                        if(previous!=null) changes++;
                        Console.WriteLine("{0:F1}s {1}",timer.Elapsed.TotalSeconds,state);
                        previous=state;
                    }
                }
                Thread.Sleep(25);
            }
            Console.WriteLine("Observed changes: {0}. No changes alone does not prove failure: the phone may have been idle.",changes);
        }
        if(found==0) Console.WriteLine("No Android HID candidate found. Connect the phone and keep the app open.");
    }
}
'@

[PocketPadProbe]::Monitor([Math]::Clamp($Seconds, 1, 60))
