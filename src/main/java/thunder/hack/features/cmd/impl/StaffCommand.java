package thunder.hack.features.cmd.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import thunder.hack.features.cmd.Command;
import thunder.hack.features.hud.impl.StaffBoard;

import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class StaffCommand extends Command {
    public static List<String> staffNames = new ArrayList<>();

    public StaffCommand() {
        super("staff");
    }

    private void syncWithStaffBoard() {
        StaffBoard.addStaffFromCommand(new ArrayList<>(staffNames));
    }

    @Override
    public void executeBuild(@NotNull LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("reset").executes(context -> {
            staffNames.clear();
            StaffBoard.clearStaffFromCommand();
            sendMessage("Staff list got reset.");
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("add").then(arg("name", StringArgumentType.word()).executes(context -> {
            String name = context.getArgument("name", String.class);

            if (!staffNames.contains(name)) {
                staffNames.add(name);
                syncWithStaffBoard();
                sendMessage(Formatting.GREEN + name + " added to staff list");
            } else {
                sendMessage(Formatting.YELLOW + name + " is already in staff list");
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("del").then(arg("name", StringArgumentType.word()).executes(context -> {
            String name = context.getArgument("name", String.class);

            if (staffNames.remove(name)) {
                syncWithStaffBoard();
                sendMessage(Formatting.GREEN + name + " removed from staff list");
            } else {
                sendMessage(Formatting.RED + name + " not found in staff list");
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("import").then(arg("names", StringArgumentType.greedyString()).executes(context -> {
            String names = context.getArgument("names", String.class);
            String[] nameArray = names.split("[,\\s]+");

            int added = 0;
            for (String name : nameArray) {
                name = name.trim();
                if (!name.isEmpty() && !staffNames.contains(name)) {
                    staffNames.add(name);
                    added++;
                }
            }

            if (added > 0) {
                syncWithStaffBoard();
                sendMessage(Formatting.GREEN + "Added " + added + " staff members to the list");
            } else {
                sendMessage(Formatting.YELLOW + "No new staff members were added");
            }

            return SINGLE_SUCCESS;
        })));

        builder.executes(context -> {
            if (staffNames.isEmpty()) {
                sendMessage(Formatting.YELLOW + "Staff list is empty. Use .staff add <name> to add manually or .staff import <names> for multiple.");
            } else {
                sendMessage(Formatting.GREEN + "Current staff list (" + staffNames.size() + " members):");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < staffNames.size(); i++) {
                    sb.append(staffNames.get(i));
                    if (i < staffNames.size() - 1) sb.append(", ");
                }
                sendMessage(Formatting.AQUA + sb.toString());
            }
            return SINGLE_SUCCESS;
        });
    }
}