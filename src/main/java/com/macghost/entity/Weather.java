package com.macghost.entity;

public class Weather {

    private String cidade;
    private double temperatura;
    private String descricao;

    public Weather(String cidade, double temperatura, String descricao) {
        validarDados(cidade, temperatura, descricao);
    }

    public void validarDados(String cidade, double temperatura, String descricao) {
        if (cidade == null || cidade.trim().isEmpty()) {
            this.cidade = "Cidade não informada";
        } else {
            this.cidade = cidade.trim();
        }

        if (Double.isNaN(temperatura) || Double.isInfinite(temperatura)) {
            this.temperatura = 0.0;
        } else {
            this.temperatura = temperatura;
        }

        if (descricao == null || descricao.trim().isEmpty()) {
            this.descricao = "Descrição não informada";
        } else {
            this.descricao = descricao.trim();
        }
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public double getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(double temperatura) {
        this.temperatura = temperatura;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

}
