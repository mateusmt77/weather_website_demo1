package com.macghost.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class APIConfigTests {

    @SuppressWarnings("unchecked")
    private Supplier<InputStream> fonteSimulada() {
        return mock(Supplier.class);
    }

    private InputStream propriedades(String conteudo) {
        return new ByteArrayInputStream(conteudo.getBytes(StandardCharsets.ISO_8859_1));
    }

    @Test
    void deveLerChaveDasPropriedadesSemArquivoReal() {
        Supplier<InputStream> fonte = fonteSimulada();
        when(fonte.get()).thenReturn(propriedades("api.key=chave-de-teste\nambiente=local\n"));

        APIConfig config = new APIConfig(fonte);

        assertEquals("chave-de-teste", config.getApiKey());
        verify(fonte, times(1)).get();
    }

    @Test
    void deveRemoverEspacosExternosDaChave() {
        Supplier<InputStream> fonte = fonteSimulada();
        when(fonte.get()).thenReturn(propriedades("api.key=  chave-teste  \n"));

        assertEquals("chave-teste", new APIConfig(fonte).getApiKey());
    }

    @Test
    void deveFecharOFluxoAposCarregar() throws Exception {
        Supplier<InputStream> fonte = fonteSimulada();
        InputStream dados = spy(propriedades("api.key=segredo-falso\n"));
        when(fonte.get()).thenReturn(dados);

        new APIConfig(fonte);

        verify(dados).close();
    }

    @Test
    void deveRejeitarArquivoNaoEncontrado() {
        Supplier<InputStream> fonte = fonteSimulada();
        when(fonte.get()).thenReturn(null);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> new APIConfig(fonte));

        assertTrue(erro.getMessage().contains("não encontrado"));
    }

    @Test
    void deveRejeitarPropriedadeAusente() {
        Supplier<InputStream> fonte = fonteSimulada();
        when(fonte.get()).thenReturn(propriedades("outra.chave=valor\n"));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> new APIConfig(fonte));

        assertTrue(erro.getMessage().contains("api.key"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"api.key=\n", "api.key=    \n"})
    void deveRejeitarChaveVaziaOuEmBranco(String conteudo) {
        Supplier<InputStream> fonte = fonteSimulada();
        when(fonte.get()).thenReturn(propriedades(conteudo));

        assertThrows(IllegalStateException.class, () -> new APIConfig(fonte));
    }

    @Test
    void devePreservarACausaDaFalhaDeLeitura() throws Exception {
        Supplier<InputStream> fonte = fonteSimulada();
        InputStream dados = mock(InputStream.class);
        when(fonte.get()).thenReturn(dados);
        when(dados.read(any(byte[].class)))
                .thenThrow(new IOException("falha de leitura simulada"));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> new APIConfig(fonte));

        assertInstanceOf(IOException.class, erro.getCause());
        verify(dados).close();
    }

    @Test
    void deveRejeitarFonteNula() {
        assertThrows(NullPointerException.class,
                () -> new APIConfig((Supplier<InputStream>) null));
    }
}
