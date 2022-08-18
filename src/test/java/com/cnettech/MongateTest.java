package com.cnettech;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class MongateTest 
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        String temp = "SYSINCNET!@,01,LOCAL,,,,1024,768,001~C:010~,00000,001~ONOTEPAD.EXE|5.1.2600~,014,011,0,,1234";
        ProcessMsg processMsg = new ProcessMsg();
        processMsg.Msg("127.0.0.1",temp);
        assertTrue( true );
    }
}
