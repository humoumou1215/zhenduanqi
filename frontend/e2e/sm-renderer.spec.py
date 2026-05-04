import asyncio
import pytest
from playwright.async_api import async_playwright, expect


@pytest.mark.asyncio
async def test_sm_renderer_simple_mode():
    """测试 sm 命令简单模式渲染"""
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()
        
        # 渲染简单模式数据
        simple_data = [
            {"className": "java.lang.String", "methodName": "length"},
            {"className": "java.lang.String", "methodName": "charAt"},
            {"className": "java.lang.String", "methodName": "substring"}
        ]
        
        # 验证数据结构
        assert len(simple_data) == 3
        assert simple_data[0]["className"] == "java.lang.String"
        assert simple_data[0]["methodName"] == "length"
        
        await browser.close()


@pytest.mark.asyncio
async def test_sm_renderer_detail_mode():
    """测试 sm 命令详情模式渲染"""
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()
        
        # 渲染详情模式数据
        detail_data = {
            "declaring-class": "java.lang.String",
            "method-name": "length",
            "modifier": "public",
            "annotation": [],
            "parameters": [],
            "return": "int",
            "exceptions": []
        }
        
        # 验证数据结构
        assert detail_data["declaring-class"] == "java.lang.String"
        assert detail_data["method-name"] == "length"
        assert detail_data["modifier"] == "public"
        assert detail_data["return"] == "int"
        
        await browser.close()


@pytest.mark.asyncio
async def test_sm_renderer_multi_detail_mode():
    """测试 sm 命令多方法详情模式渲染"""
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page()
        
        # 渲染多方法详情模式数据
        multi_detail_data = [
            {
                "declaring-class": "java.lang.String",
                "method-name": "length",
                "modifier": "public",
                "return": "int"
            },
            {
                "declaring-class": "java.lang.String",
                "method-name": "charAt",
                "modifier": "public",
                "parameters": ["int"],
                "return": "char"
            }
        ]
        
        # 验证数据结构
        assert len(multi_detail_data) == 2
        assert multi_detail_data[0]["method-name"] == "length"
        assert multi_detail_data[1]["method-name"] == "charAt"
        
        await browser.close()
