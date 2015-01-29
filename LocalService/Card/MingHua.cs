using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Runtime.InteropServices;

namespace Card
{
    //明华动态库调用
    public static class MingHua
    {
        #region 明华动态库导入
        //初始化串口
        [DllImport("Mwic_32.dll", EntryPoint = "ic_init", CallingConvention = CallingConvention.StdCall)]
        public static extern int ic_init(Int16 com, Int32 baut);
        //关闭串口
        [DllImport("Mwic_32.dll", EntryPoint = "ic_exit", CallingConvention = CallingConvention.StdCall)]
        public static extern int ic_exit(int icdev);

        //测试24c02卡
        [DllImport("Mwic_32.dll", EntryPoint = "chk_24c02", CallingConvention = CallingConvention.StdCall)]
        public static extern int chk_24c02(int icdev);
        //读24c02卡
        [DllImport("Mwic_32.dll", EntryPoint = "srd_24c02", CallingConvention = CallingConvention.StdCall)]
        public static extern int srd_24c02(int icdev, Int16 offset, Int16 len, byte[] data_buffer);
        //写24c02卡
        [DllImport("Mwic_32.dll", EntryPoint = "swr_24c02", CallingConvention = CallingConvention.StdCall)]
        public static extern int swr_24c02(int icdev, Int16 offset, Int16 len, byte[] data_buffer);

        //测试24c01a卡
        [DllImport("Mwic_32.dll", EntryPoint = "chk_24c01a", CallingConvention = CallingConvention.StdCall)]
        public static extern int chk_24c01a(int icdev);
        //读24c01a卡
        [DllImport("Mwic_32.dll", EntryPoint = "srd_24c01a", CallingConvention = CallingConvention.StdCall)]
        public static extern int srd_24c01a(int icdev, Int16 offset, Int16 len, byte[] data_buffer);
        //写24c01a卡
        [DllImport("Mwic_32.dll", EntryPoint = "swr_24c01a", CallingConvention = CallingConvention.StdCall)]
        public static extern int swr_24c01a(int icdev, Int16 offset, Int16 len, byte[] data_buffer);

        //测试4442卡
        [DllImport("Mwic_32.dll", EntryPoint = "chk_4442", CallingConvention = CallingConvention.StdCall)]
        public static extern int chk_4442(int icdev);
        //校验密码
        [DllImport("Mwic_32.dll", EntryPoint = "csc_4442", CallingConvention = CallingConvention.StdCall)]
        public static extern int csc_4442(int icdev, Int16 len, byte[] data_buffer);
        //读4442卡
        [DllImport("Mwic_32.dll", EntryPoint = "srd_4442", CallingConvention = CallingConvention.StdCall)]
        public static extern int srd_4442(int icdev, Int16 offset, Int16 len, byte[] data_buffer);
        //写4442卡
        [DllImport("Mwic_32.dll", EntryPoint = "swr_4442", CallingConvention = CallingConvention.StdCall)]
        public static extern int swr_4442(int icdev, Int16 offset, Int16 len, byte[] data_buffer);
        #endregion

        //清除新卡卡上内容，4442卡默认密码ffffff，只做了24c01a，24c02, 4442卡
        public static int ClearCard(short com, int baud)
        {
	        //打开串口
	        int icdev = ic_init(com, baud);
	        if (icdev < 0)
	        {
		        return -1;
	        }
	        //清除卡上内容
	        if(chk_24c02(icdev) == 0)
	        {
		        byte[] data_buffer = new byte[0x100];
                for(int i = 0; i < data_buffer.Length; i++)
                {
                    data_buffer[i] = 0xff;
                }
		        swr_24c02(icdev, 0, 0x100, data_buffer);
	        }
	        else if(chk_24c01a(icdev) == 0)
	        {
		        byte[] data_buffer = new byte[0x80];
                for(int i = 0; i < data_buffer.Length; i++)
                {
                    data_buffer[i] = 0xff;
                }
		        swr_24c01a(icdev, 0, 0x80, data_buffer);
	        }
	        else if(chk_4442(icdev) == 0)
	        {
		        byte[] passwd = new byte[3] {0xff, 0xff, 0xff};
		        csc_4442(icdev, 3, passwd);
		        byte[] data_buffer = new byte[0xe0];
                for(int i = 0; i < data_buffer.Length; i++)
                {
                    data_buffer[i] = 0xff;
                }
		        swr_4442(icdev, 0x20, 0xe0, data_buffer);
	        }
	        //关闭串口
	        ic_exit(icdev);
            return 0;
        }

        //看是否4442卡
        public static int Is4442(short com, int baud)
        {
            //打开串口
            int icdev = ic_init(com, baud);
            if (icdev < 0)
            {
                return -1;
            }
            int ret = chk_4442(icdev);
            //关闭串口
            ic_exit(icdev);
            return ret;
        }
    }
}
