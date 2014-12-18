using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Runtime.InteropServices;
using Com.Aote.Logs;

namespace Card
{
    public class JinKaGYMHK : ICard
    {
        private static Log Log = Log.GetInstance("Card.JinKaGYMHK");

        public string Test()
        {
            return "ruisen";
        }

        #region 金卡动态库导入
        //读卡，标准接口
        [DllImport("goldcard.dll", EntryPoint = "ReadGasCard", CallingConvention = CallingConvention.StdCall)]
        public static extern int StaticReadGasCard(Int16 com, Int32 baut,byte[] kmm, ref Int16 klx, ref Int16 kzt,  
            byte[] kh, byte[] dqdm, byte[] yhh, byte[] tm, ref Int32 ql, ref Int16 cs, ref Int32 ljgql, ref Int16 bkcs, 
            ref Int32 ljyql, ref Int32 syql, ref Int32 bjql, ref Int32 czsx, ref Int32 tzed,byte[] sqrq ,ref Int32 oldprice, 
            ref Int32 newprice, byte[] sxrq, byte[] sxbj);
        //写新卡，标准接口
        [DllImport("goldcard.dll", EntryPoint = "WriteNewCard", CallingConvention = CallingConvention.StdCall)]
        public static extern int StaticWriteNewCard(Int16 com, Int32 baut, byte[] kmm, Int16 klx, Int16 kzt,
            byte[] kh, byte[] dqdm, byte[] yhh, byte[] tm, Int32 ql, Int16 cs, Int32 ljgql, Int16 bkcs, Int32 ljyql,
            Int32 bjql, Int32 czsx, Int32 tzed, byte[] sqrq, ref Int32 oldprice, ref Int32 newprice, byte[] sxrq, byte[] sxbj);
        //测卡，标准接口
        [DllImport("goldcard.dll", EntryPoint = "CheckGasCard", CallingConvention = CallingConvention.StdCall)]
        public static extern int StaticCheckGasCard(Int16 com, Int32 baut);
        //格式化卡，标准接口
        [DllImport("goldcard.dll", EntryPoint = "FormatGasCard", CallingConvention = CallingConvention.StdCall)]
        public static extern int StaticFormatGasCard(Int16 com, Int32 baut, byte[] kmm, Int16 klx, byte[] kh, byte[] dqdm);
        //写购气卡，标准接口
        [DllImport("goldcard.dll", EntryPoint = "WriteGasCard", CallingConvention = CallingConvention.StdCall)]
        public static extern int StaticWriteGasCard(Int16 com, Int32 baut, byte[] kmm, Int16 klx, byte[] kh, 
            byte[] dqdm, Int32 ql, Int16 cs, Int32 ljgql, Int32 bjql, Int32 czsx, Int32 tzed,
            byte[] sqrq ,ref Int32 oldprice, ref Int32 newprice, byte[] sxrq, byte[] sxbj);
        #endregion

        #region ICard Members

        public int CheckGasCard(short com, int baud)
        {
            int ret = -1;
            try 
            {
                ret = StaticCheckGasCard(com, baud);
                Log.Debug("check card ret=" + ret);
                if (ret == 0)
                {
                    byte[] cardNO = new byte[100];
                    byte[] kmm = new byte[100];
                    byte[] dqdm = new byte[100];
                    byte[] yhh = new byte[100];
                    byte[] sqrq = new byte[100];
                    byte[] sxrq = new byte[100];
                    byte[] sxbj = new byte[100];
                    byte[] tm = new byte[100];
                    short klx = 0;
                    short kzt = 0;
                    int ljgql = 0;
                    int ljyql = 0;
                    int ql = 0;
                    short cs = 0;
                    short bkcs = 0;
                    int syql = 0;
                    int bjql = 0;
                    int czsx = 0;
                    int tzed = 0;
                    int oldprice = 0;
                    int newprice = 0;
                    ret = StaticReadGasCard(0, baud, kmm, ref klx, ref kzt, cardNO, dqdm, yhh, tm,
                   ref ql, ref cs, ref ljgql, ref bkcs, ref ljyql, ref syql, ref bjql, ref czsx,
                   ref tzed, sqrq, ref oldprice, ref newprice, sxrq, sxbj);
                    if (klx == 2)
                    {
                        Log.Debug("此卡是金卡工业卡！");
                        return 0;
                    }
                    Log.Debug("此卡不是金卡工业卡！");
                    return -1;
                }
                Log.Debug("此卡不是金卡工业卡！");
                return -1;
            }
            catch(Exception e)
            {
                Log.Debug("金卡工业判卡异常：" + e.Message + "--" + e.StackTrace);
            }
            return ret;
        }

        public int FormatGasCard(short com, int baud, string kmm, string kh, string dqdm)
        {
            int ret = -1;
            try
            {
                byte[] mm = new byte[10];
                byte[] cardNO = System.Text.Encoding.GetEncoding(1252).GetBytes(kh);
                byte[] bdqdm = System.Text.Encoding.GetEncoding(1252).GetBytes("0577");
                Log.Debug("JinKaGY FormatGasCard start");
                Log.Debug("kmm --" + mm + "kh --" + cardNO);
                ret = StaticFormatGasCard(com, baud, mm, 2, cardNO, bdqdm);
                Log.Debug("JinKaGY FormatGasCard end,return:" + ret);
                if (0 == ret)
                {
                    Log.Debug("金卡工业擦卡成功！");
                    return 0;
                }
                else
                {
                    Log.Debug("金卡工业擦卡失败！");
                    return -1;
                }
            }
            catch (Exception e)
            {
                Log.Debug("金卡工业擦卡异常：" + e.Message + "--" + e.StackTrace);
            }
            return ret;
        }

        public int WriteGasCard(short com, int baud, ref string kmm, string kh, string dqdm, int ql, int csql, int ccsql, short cs, int ljgql, int bjql, int czsx, int tzed, string sqrq, string cssqrq, int oldprice, int newprice, string sxrq, string sxbj)
        {
            int ret = -1;
            try
            {
                byte[] mm = new byte[10];
                byte[] cardNO = System.Text.Encoding.GetEncoding(1252).GetBytes(kh);
                byte[] bdqdm = System.Text.Encoding.GetEncoding(1252).GetBytes("0577");
                byte[] bsqrq = new byte[10];
                byte[] bsxrq = new byte[10];
                byte[] bsxbj = new byte[10];
                Int32 boldprice = 0;
                Int32 bnewprice = 0;
                Log.Debug("JinKaGY WriteGasCard start");
                Log.Debug("kh：" + cardNO + "klx：" + 2 + "ql：" + ql + "cs：" + cs);
                ret = StaticWriteGasCard(com, baud, mm, 2, cardNO, bdqdm, ql,
                    cs, ljgql, bjql, czsx, tzed, bsqrq, ref boldprice, ref bnewprice, bsxrq, bsxbj);
                Log.Debug("JinKaGY WriteGasCard end,return:" + ret);
                if (0 == ret)
                {
                    Log.Debug("金卡工业购气成功！");
                    return 0;
                }
                else
                {
                    Log.Debug("金卡工业购气失败！");
                    return -1;
                }
            }
            catch (Exception e)
            {
                Log.Debug("金卡工业购气卡异常：" + e.Message + "--" + e.StackTrace);
            }
            return ret;
        }

        public string Name
        {
            get
            {
                return "jinkagymhk";
            }
        }

        public int ReadGasCard(short com, int baud, ref string kh, ref int ql, ref decimal money, ref short cs, ref short bkcs, ref string dqdm)
        {
            int ret = -1;
            try
            {
                byte[] cardNO = new byte[100];
                byte[] kmm = new byte[100];
                byte[] bdqdm = System.Text.Encoding.GetEncoding(1252).GetBytes(dqdm);
                byte[] yhh = new byte[100];
                byte[] sqrq = new byte[100];
                byte[] sxrq = new byte[100];
                byte[] sxbj = new byte[100];
                byte[] tm = new byte[100];
                short klx = 0;
                short kzt = 0;
                int ljgql = 0;
                int ljyql = 0;
                int syql = 0;
                int bjql = 0;
                int czsx = 0;
                int tzed = 0;
                int oldprice = 0;
                int newprice = 0;
                Log.Debug("JinKaGY ReadGasCard start");
                ret = StaticReadGasCard(0, baud, kmm, ref klx, ref kzt, cardNO, bdqdm, yhh, tm,
                    ref ql, ref cs, ref ljgql, ref bkcs, ref ljyql, ref syql, ref bjql, ref czsx,
                    ref tzed, sqrq, ref oldprice, ref newprice, sxrq, sxbj);
                //卡号转换成字符串
                cardNO[8] = 0;
                kh = Encoding.ASCII.GetString(cardNO, 0, 8);
                Log.Debug("JinKaGY ReadGasCard end,return:" + ret);
                if (0 == ret || 2 == klx)
                {
                    Log.Debug("金卡工业读卡成功！");
                    Log.Debug("kh：" + kh + "ql：" + ql + "klx：" + klx + "cs：" + cs);
                    return 0;
                }
                else
                {
                    Log.Debug("金卡工业读卡失败！");
                    return -1;
                }
            }
            catch (Exception e)
            {
                Log.Debug("金卡工业读卡异常：" + e.Message + "--" + e.StackTrace);
            }
            return ret;
        }

        public int WriteNewCard(short com, int baud, ref string kmm, short kzt, string kh, string dqdm, string yhh, string tm, int ql, int csql, int ccsql, short cs, int ljgql, short bkcs, int ljyql, int bjql, int czsx, int tzed, string sqrq, string cssqrq, int oldprice, int newprice, string sxrq, string sxbj)
        {
            int ret = -1;
            try
            {
                byte[] rcardNO = new byte[100];
                byte[] rkmm = new byte[100];
                byte[] rdqdm = new byte[100];
                byte[] ryhh = new byte[100];
                byte[] rsqrq = new byte[100];
                byte[] rsxrq = new byte[100];
                byte[] rsxbj = new byte[100];
                byte[] rtm = new byte[100];
                short klx = 0;
                short rkzt = 0;
                int rljgql = 0;
                int rql = 0;
                Int16 rcs = 0;
                Int16 rbkcs = 0;
                int rljyql = 0;
                int rsyql = 0;
                int rbjql = 0;
                int rczsx = 0;
                int rtzed = 0;
                int roldprice = 0;
                int rnewprice = 0;
                Log.Debug("JinKa ReadGasCard start");
                ret = StaticReadGasCard(com, baud, rkmm, ref klx, ref rkzt, rcardNO, rdqdm, ryhh, rtm,
                    ref rql, ref rcs, ref rljgql, ref rbkcs, ref rljyql, ref rsyql, ref rbjql, ref rczsx,
                    ref rtzed, rsqrq, ref roldprice, ref rnewprice, rsxrq, rsxbj);
                Log.Debug("JinKa ReadGasCard end,return:" + ret);
                byte[] mm = new byte[10];
                byte[] cardNO = System.Text.Encoding.GetEncoding(1252).GetBytes(kh);
                byte[] bdqdm = System.Text.Encoding.GetEncoding(1252).GetBytes("0577");
                byte[] byhh = System.Text.Encoding.GetEncoding(1252).GetBytes("0000000001");
                byte[] btm = new byte[10];
                byte[] bsqrq = new byte[10];
                byte[] bsxrq = new byte[10];
                byte[] bsxbj = new byte[10];
                Int32 boldprice = 0;
                Int32 bnewprice = 0;
                if (0 == ret)
                {
                    Log.Debug("JinKa FormatGasCard start");
                    //发卡前先格式化卡
                    ret = StaticFormatGasCard(com, baud, rkmm, klx, rcardNO, rdqdm);
                    Log.Debug("JinKa FormatGasCard end,return:" + ret);
                    Log.Debug("JinKa WriteNewCard start");
                    Log.Debug("kh：" + cardNO + "ql:" + ql + "cs" + cs);
                    ret = StaticWriteNewCard(com, baud, mm, 2, kzt, cardNO, bdqdm, byhh, btm,
                        ql, cs, ljgql, bkcs, ljyql, bjql, czsx, tzed, bsqrq, ref boldprice, ref bnewprice, bsxrq, bsxbj);
                    Log.Debug("JinKa WriteNewCard end,return:" + ret);
                }
                else
                {
                    Log.Debug("JinKaGY WriteNewCard start");
                    Log.Debug("kh：" + cardNO + "klx:" + 2 + "ql:" + ql + "cs" + cs);
                    ret = StaticWriteNewCard(com, baud, mm, 2, kzt, cardNO, bdqdm, byhh, btm,
                        ql, cs, ljgql, bkcs, ljyql, bjql, czsx, tzed, bsqrq, ref boldprice, ref bnewprice, bsxrq, bsxbj);
                    Log.Debug("JinKaGY WriteNewCard end,return:" + ret);
                }
                if (0 == ret)
                {
                    Log.Debug("金卡工业写新卡成功！");
                    return 0;
                }
                else
                {
                    Log.Debug("金卡工业写新卡失败！");
                    return -1;
                }
            }
            catch (Exception e)
            {
                Log.Debug("金卡工业写新卡异常：" + e.Message + "--" + e.StackTrace);
            }
            return ret;
        }
        #endregion       
    }
}
