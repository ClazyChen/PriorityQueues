# 用于构建 verilog 代码的命令行工具

import os
import sys
import argparse
import shutil
current_dir = os.path.dirname(os.path.abspath(__file__))
verilog_dir = os.path.join(current_dir, "verilog")
template_main_scala = '''
package {package_name}

import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.{{ChiselStage, FirtoolOption}}

object Main extends App {{
    println("Generating the hardware")
    (new ChiselStage).execute(
        Array("--target", "systemverilog"),
        Seq(ChiselGeneratorAnnotation(() => new {module_name}),
            FirtoolOption("--disable-all-randomization"),
            FirtoolOption("-preserve-aggregate=vec"),
            )
    )
    println("Done")
}}
'''
template_test_scala = '''
package {package_name}

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import fpga.BlackBox._

class {module_name}Test extends AnyFlatSpec with ChiselScalatestTester {{
    behavior of "{module_name}"
    it should "work" in {{
        test(new {module_name}) {{ c =>
            test_black_box(c)
        }}
    }}
}}
'''

# 正常命令行，黄色字体
def print_yellow(text, end="\n"):
    print(f"\033[93m{text}\033[0m", end=end)

# 错误命令行，红色字体
def print_red(text, end="\n"):
    print(f"\033[91m{text}\033[0m", end=end)

# 脚本命令行，绿色字体
def print_green(text, end="\n"):
    print(f"\033[92m{text}\033[0m", end=end)

in_script = False
def print_auto(text, end="\n"):
    if in_script:
        print_green(text, end=end)
    else:
        print_yellow(text, end=end)

def print_error(text, end="\n"):
    print_red(text, end=end)

def print_info(text, end="\n"):
    print_yellow(text, end=end)

# 解析路径
def parse_path(path):
    # 如果 path 是绝对路径，则使用 path
    if os.path.isabs(path):
        return path
    # 如果 path 是相对路径，则使用 current_dir 和 path 拼接
    return os.path.join(current_dir, path)

# cd 命令，切换到指定目录
# cd path [-c/--create]
# -c/--create 参数表示如果路径不存在，则创建该路径
def cd(*args):
    parser = argparse.ArgumentParser()
    parser.add_argument("path", type=str)
    parser.add_argument("-c", "--create", action="store_true")
    args = parser.parse_args(args)
    global current_dir
    current_dir = parse_path(args.path)
    # 如果路径是一个文件，则切换到文件所在的目录
    if os.path.isfile(current_dir):
        current_dir = os.path.dirname(current_dir)
    # 如果路径不存在，则根据 create 参数决定是否创建
    if not os.path.exists(current_dir):
        if args.create:
            os.makedirs(current_dir)
        else:
            print_error(f"Path {current_dir} does not exist")
            return False
    return True

# ls 命令，列出当前目录下的所有文件和目录
def ls():
    for file in os.listdir(current_dir):
        print_auto(file)

# exit 命令，退出脚本
def exit():
    sys.exit(0)

# 解析模块名
def parse_module_name(module_name):
    # 模块名的格式为 [package_name.]module_name[(module_args)]
    # 如果 package_name 为空，则采用默认 package_name = 'fpga'
    # module_name 不能为空
    # module_args 可以省略、为空、或为 , 分隔的参数列表
    # 返回 package_name, module_name, module_args
    if '(' in module_name:
        module_name, module_args = module_name.split('(')
        module_args = module_args.rstrip(')')
        module_args = module_args.split(',')
        module_args = [x.strip() for x in module_args if len(x.strip()) > 0]
    else:
        module_args = []
    # 解析 package_name
    if '.' in module_name:
        package_name = '.'.join(module_name.split('.')[:-1])
        module_name = module_name.split('.')[-1]
    else:
        package_name = 'fpga'
    return package_name, module_name, module_args

# build 命令，构建 verilog 代码
# build module_name [-s/--split] [-c/--clean] [-r/--remove-comment] [-o/--output output_path]
# -s/--split 参数表示将 verilog 代码分割为多个文件
# -c/--clean 参数表示清除之前的构建结果
# -r/--remove-comment 参数表示移除 Verilog 代码中的注释
# -o/--output 参数表示输出路径
def build(*args):
    parser = argparse.ArgumentParser()
    parser.add_argument("module_name", type=str)
    parser.add_argument("-s", "--split", action="store_true")
    parser.add_argument("-c", "--clean", action="store_true")
    parser.add_argument("-r", "--remove-comment", action="store_true")
    parser.add_argument("-o", "--output", type=str)
    args = parser.parse_args(args)
    package_name, module_name, module_args = parse_module_name(args.module_name)
    # 如果当前目录下没有 build.sbt 文件，则报告错误
    if not os.path.exists(os.path.join(current_dir, "build.sbt")):
        print_error("No build.sbt file found in current directory")
        return False
    # 如果当前目录下没有 src/main/scala 目录，则报告错误
    scala_dir = os.path.join(current_dir, "src", "main", "scala")
    if not os.path.exists(scala_dir):
        print_error("No src/main/scala directory found in current directory")
        return False
    # 如果 scala_dir 下没有 package_name 对应的目录，则报告错误
    package_dir = os.path.join(scala_dir, *package_name.split('.'))
    if not os.path.exists(package_dir):
        print_error(f"No {package_name} package found in current directory")
        return False
    # 如果 package_dir 下没有 module_name 对应的文件，则报告错误
    module_file = os.path.join(package_dir, module_name + ".scala")
    if not os.path.exists(module_file):
        print_error(f"No {module_name} module found in current directory")
        return False
    # 如果 output 参数为空，则采用 verilog_dir/package_name 作为输出路径
    if args.output is None:
        output_dir = os.path.join(verilog_dir, *package_name.split('.'))
    else:
        output_dir = args.output
    # 如果 output_dir 不存在，则创建该目录
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    # 如果 args.clean 参数为真，则清除 output_dir 下的所有文件
    if args.clean:
        for file in os.listdir(output_dir):
            if os.path.isfile(os.path.join(output_dir, file)):
                os.remove(os.path.join(output_dir, file))
    # 在当前目录下生成 Main.scala 文件
    with open(os.path.join(current_dir, "Main.scala"), "w") as f:
        module_name_with_args = module_name
        if len(module_args) > 0:
            module_name_with_args += f"({', '.join(module_args)})"
        f.write(template_main_scala.format(package_name=package_name, module_name=module_name_with_args))
    # 进入当前目录并运行 sbt，然后回到 builder.py 所在目录
    os.chdir(current_dir)
    os.system(f'sbt "runMain {package_name}.Main"')
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    # 删除当前目录下的 Main.scala 文件
    os.remove(os.path.join(current_dir, "Main.scala"))
    # 生成的 verilog 文件
    verilog_file = os.path.join(current_dir, f"{module_name}.sv")
    # 如果 verilog 文件不存在，则报告错误
    if not os.path.exists(verilog_file):
        print_error(f"No {module_name}.sv file found in current directory")
        return False
    # 如果需要移除注释，则移除 verilog 文件中的注释
    if args.remove_comment:
        with open(verilog_file, "r") as f:
            verilog_content = f.readlines()
        with open(verilog_file, "w") as f:
            for line in verilog_content:
                if "//" in line:
                    line = line[:line.index("//")] + "\n"
                f.write(line)
    # 如果需要分割文件，对每个 module 生成对应的 verilog 文件
    if args.split:
        # 读取 verilog 文件
        with open(verilog_file, "r") as f:
            verilog_content = f.readlines()
        module_name = None
        for line in verilog_content:
            if len(line.strip()) > 0:
                if line.strip().startswith("module"):
                    module_name = line.strip().split(" ")[1].split("(")[0]
                    with open(os.path.join(output_dir, f"{module_name}.sv"), "w") as f:
                        f.write(line)
                elif module_name is not None:
                    with open(os.path.join(output_dir, f"{module_name}.sv"), "a") as f:
                        f.write(line)
        # 删除 verilog 文件
        os.remove(verilog_file)
    else:
        # 将生成的 verilog 文件从当前目录移动到 output_dir
        shutil.move(verilog_file, os.path.join(output_dir, f"{module_name}.sv"))
    # 删除当前目录下的所有 firrtl 文件和 json 文件
    for file in os.listdir(current_dir):
        if file.endswith(".fir") or file.endswith(".json"):
            os.remove(os.path.join(current_dir, file))
    # 输出构建结束的提示
    print_green("success: build finished")
    return True

# test 命令，测试指定模块【针对 PQ 特别设计】
# test module_name [-w/--wave] [-c/--clean]
# -w/--wave 参数表示生成波形文件
# -c/--clean 参数表示清除之前的测试结果
def test(*args):
    parser = argparse.ArgumentParser()
    parser.add_argument("module_name", type=str)
    parser.add_argument("-w", "--wave", action="store_true")
    parser.add_argument("-c", "--clean", action="store_true")
    args = parser.parse_args(args)
    package_name, module_name, module_args = parse_module_name(args.module_name)
    test_run_dir = os.path.join(current_dir, "test_run_dir")
    if not os.path.exists(test_run_dir):
        os.makedirs(test_run_dir)
    # 如果 args.clean 参数为真，则清除 test_run_dir 下的所有文件
    if args.clean:
        shutil.rmtree(test_run_dir)
    # 如果当前目录下没有 build.sbt 文件，则报告错误
    if not os.path.exists(os.path.join(current_dir, "build.sbt")):
        print_error("No build.sbt file found in current directory")
        return False
    # 如果当前目录下没有 src/test/scala 目录，则报告错误
    test_scala_dir = os.path.join(current_dir, "src", "test", "scala")
    if not os.path.exists(test_scala_dir):
        print_error("No src/test/scala directory found in current directory")
        return False
    # 如果 test_scala_dir 下没有 package_name 对应的目录，则创建该目录
    test_dir = os.path.join(test_scala_dir, *package_name.split('.'))
    if not os.path.exists(test_dir):
        os.makedirs(test_dir)
    # 生成对应的测试文件
    with open(os.path.join(test_dir, f"{module_name}Test.scala"), "w") as f:
        f.write(template_test_scala.format(package_name=package_name, module_name=module_name))
    # 如果 args.wave 参数为真，则添加 -DwriteVcd=1 参数
    sbt_options = []
    if args.wave:
        sbt_options.append("-DwriteVcd=1")
    # 运行 sbt，然后回到 builder.py 所在目录
    os.chdir(current_dir)
    os.system(f'sbt "testOnly {package_name}.{module_name}Test {"" if len(sbt_options) == 0 else "-- " + " ".join(sbt_options)}"')
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    # 删除 生成的测试文件
    os.remove(os.path.join(test_dir, f"{module_name}Test.scala"))
    # 输出测试结束的提示
    print_green("success: test finished")
    return True

# run 命令，运行指定文件
def run(path):
    global in_script
    path = parse_path(path)
    if not os.path.exists(path):
        print_error(f"File {path} does not exist")
        return False
    if os.path.isdir(path):
        print_error(f"Path {path} is a directory, not a file")
        return False
    with open(path, "r") as f:
        lines = f.readlines()
    in_script = True
    for line in lines:
        line = line.strip()
        if len(line) == 0 or line.startswith("#"):
            continue
        print_auto(" > " + line)
        execute(line)
    in_script = False

# help 命令，显示帮助信息
def help():
    print_auto('''
    cd [path]                    切换到指定目录
        [-c/--create]             如果路径不存在，则创建该路径
    ls                           列出当前目录下的所有文件和目录
    run [path]                   运行指定文件
    build [module_name]          构建指定模块
        [-s/--split]              将 verilog 代码分割为多个文件
        [-c/--clean]              清除之前的构建结果
        [-r/--remove-comment]     移除 Verilog 代码中的注释
        [-o/--output output_path] 指定输出路径
    test [module_name]           测试指定模块
        [-w/--wave]               生成波形文件
        [-c/--clean]              清除之前的测试结果
    ! [command]                  执行 shell 命令
    # [comment]                  打印注释
    help                         显示帮助信息
    exit                         退出脚本
    ''')

# 执行命令
def execute(command):
    command_dict = {
        "cd": cd,
        "ls": ls,
        "run": run,
        "build": build,
        "test": test,
        "help": help,
        "exit": exit,
        "quit": exit,
    }
    command = command.strip().split(" ")
    command = [x.strip() for x in command if len(x.strip()) > 0]
    if len(command) == 0:
        return
    command_name = command[0]
    command_args = command[1:]
    if command_name in command_dict:
        try:
            command_dict[command_name](*command_args)
        except Exception as e:
            print_error(f"Error executing command {command_name}: {e}")
    elif command_name.startswith("!"): # 执行 shell 命令
        try:
            os.system(' '.join(command)[1:])
        except Exception as e:
            print_error(f"Error executing command {command_name}: {e}")
    elif command_name.startswith("#"): # 打印注释
        print_auto(command_name[1:])
    else:
        print_error(f"Unknown command {command_name}")

# 主函数
def main():
    while True:
        print_auto(current_dir + " > ", end="")
        command = input()
        execute(command)

if __name__ == "__main__":
    # 如果命令行参数是文件，则运行该文件
    if len(sys.argv) > 1:
        run(sys.argv[1])
    else:
        main()

